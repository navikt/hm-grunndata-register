package no.nav.hm.grunndata.register.product

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.micronaut.security.authentication.Authentication
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.agreement.DelkontraktData
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistration
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistrationRepository
import no.nav.hm.grunndata.register.catalog.CatalogImport
import no.nav.hm.grunndata.register.catalog.CatalogImportRepository
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistration
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationRepository
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.series.SeriesRegistrationRepository
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@MicronautTest(transactional = false)
class ProductRegistrationBulkVariantHmsTest(
    private val service: ProductRegistrationService,
    private val factory: ProductRegistrationTestFactory,
    private val products: ProductRegistrationRepository,
    private val series: SeriesRegistrationRepository,
    private val catalogs: CatalogImportRepository,
    private val agreements: ProductAgreementRegistrationRepository,
    private val posts: DelkontraktRegistrationRepository,
) {
    private val admin = Authentication.build("admin", listOf(Roles.ROLE_ADMIN), emptyMap())
    private lateinit var product: ProductRegistration
    private lateinit var other: ProductRegistration
    private lateinit var catalog: CatalogImport
    private lateinit var unrelatedCatalogs: List<CatalogImport>
    private lateinit var agreement: ProductAgreementRegistration
    private val postId = UUID.randomUUID()

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @BeforeEach
    fun createFixtures(): Unit = runBlocking {
        product = factory.createTestProduct(hmsArtNr = "123456")
        other = factory.createTestProduct(
            supplierId = product.supplierId, seriesUUID = product.seriesUUID, hmsArtNr = "654321",
        )
        val agreementId = UUID.randomUUID()
        posts.save(DelkontraktRegistration(id = postId, agreementId = agreementId, delkontraktData = DelkontraktData()))
        agreement = agreements.save(
            ProductAgreementRegistration(
                productId = product.id, title = "Agreement", articleName = product.articleName,
                supplierId = product.supplierId, supplierRef = product.supplierRef, hmsArtNr = product.hmsArtNr,
                agreementId = agreementId, reference = "test", post = 1, rank = 1, postId = postId,
                createdBy = REGISTER,
            ),
        )
        catalog = catalogs.save(
            CatalogImport(
                agreementAction = "", orderRef = UUID.randomUUID().toString(), hmsArtNr = "123456",
                iso = "", title = "Catalog", supplierId = product.supplierId, supplierRef = product.supplierRef,
                reference = "test", postNr = "1", dateFrom = LocalDate.now(), dateTo = LocalDate.now().plusYears(1),
                articleAction = "", articleType = "", functionalChange = "", forChildren = "",
                supplierName = "", supplierCity = "", mainProduct = true, sparePart = false, accessory = false,
                agreementId = agreementId,
            ),
        )
        unrelatedCatalogs = listOf(
            catalogs.save(
                catalog.copy(
                    id = UUID.randomUUID(), supplierId = UUID.randomUUID(), orderRef = UUID.randomUUID().toString(),
                ),
            ),
            catalogs.save(
                catalog.copy(
                    id = UUID.randomUUID(), supplierRef = other.supplierRef, orderRef = UUID.randomUUID().toString(),
                ),
            ),
        )
        product = products.findById(product.id)!!
        other = products.findById(other.id)!!
        agreement = agreements.findById(agreement.id)!!
    }

    @AfterEach
    fun removeFixtures(): Unit = runBlocking {
        agreements.deleteById(agreement.id)
        posts.deleteById(postId)
        (unrelatedCatalogs + catalog).forEach { catalogs.deleteById(it.id) }
        products.deleteById(product.id)
        products.deleteById(other.id)
        series.deleteById(product.seriesUUID)
    }

    @Test
    fun `HMS changes and clearing update only the product's own relations`(): Unit = runBlocking {
        listOf(" 234567 " to "234567", "345678" to "345678", "   " to null).forEach { (input, expected) ->
            val result = service.bulkUpdateVariants(
                BulkVariantUpdateDTO(listOf(VariantUpdate(product.id, emptyList(), hmsArtNr = input))),
                admin,
            )
            result.failed.shouldBeEmpty()
            result.updated.single().hmsArtNr shouldBe expected
            products.findById(product.id)!!.hmsArtNr shouldBe expected
            agreements.findById(agreement.id)!!.hmsArtNr shouldBe expected
            catalogs.findById(catalog.id)!!.hmsArtNr shouldBe (expected ?: "345678")
            unrelatedCatalogs.forEach { catalogs.findById(it.id) shouldBe it }
            products.findById(other.id) shouldBe other
        }
    }

    @Test
    fun `clearing multiple HMS numbers preserves catalog numbers on the same order`(): Unit = runBlocking {
        val otherCatalog = catalogs.save(
            catalog.copy(id = UUID.randomUUID(), supplierRef = other.supplierRef, hmsArtNr = other.hmsArtNr!!),
        )
        unrelatedCatalogs = unrelatedCatalogs + otherCatalog

        val result = service.bulkUpdateVariants(
            BulkVariantUpdateDTO(
                listOf(product, other).map { VariantUpdate(it.id, emptyList(), hmsArtNr = "") },
            ),
            admin,
        )
        result.failed.shouldBeEmpty()
        result.updated.size shouldBe 2
        result.updated.forEach { it.hmsArtNr shouldBe null }
        agreements.findById(agreement.id)!!.hmsArtNr shouldBe null
        catalogs.findById(catalog.id) shouldBe catalog
        catalogs.findById(otherCatalog.id) shouldBe otherCatalog
    }

    @Test
    fun `admin changes supplier reference on published products together with HMS and preserves relations`(): Unit = runBlocking {
        products.update(product.copy(published = LocalDateTime.now()))
        val result = service.bulkUpdateVariants(
            BulkVariantUpdateDTO(
                listOf(VariantUpdate(product.id, emptyList(), hmsArtNr = "234567", supplierRef = "  new-ref  ")),
            ),
            admin,
        )
        result.failed.shouldBeEmpty()
        result.updated.single().supplierRef shouldBe "new-ref"
        products.findById(product.id)!!.supplierRef shouldBe "new-ref"
        catalogs.findById(catalog.id)!!.supplierRef shouldBe "new-ref"
        catalogs.findById(catalog.id)!!.hmsArtNr shouldBe "234567"
        agreements.findById(agreement.id)!!.supplierRef shouldBe "new-ref"
        agreements.findById(agreement.id)!!.hmsArtNr shouldBe "234567"
        unrelatedCatalogs.forEach { catalogs.findById(it.id) shouldBe it }

        val refOnly = service.bulkUpdateVariants(
            BulkVariantUpdateDTO(listOf(VariantUpdate(product.id, emptyList(), supplierRef = "next-ref"))),
            admin,
        )
        refOnly.failed.shouldBeEmpty()
        refOnly.updated.single().hmsArtNr shouldBe "234567"
        catalogs.findById(catalog.id)!!.hmsArtNr shouldBe "234567"
        catalogs.findById(catalog.id)!!.supplierRef shouldBe "next-ref"
        agreements.findById(agreement.id)!!.hmsArtNr shouldBe "234567"
        agreements.findById(agreement.id)!!.supplierRef shouldBe "next-ref"
    }

    @Test
    fun `supplier can change reference of an unpublished product with an active agreement`(): Unit = runBlocking {
        val supplier = Authentication.build("supplier", listOf(Roles.ROLE_SUPPLIER), emptyMap())
        val result = service.bulkUpdateSupplierVariants(
            BulkVariantUpdateDTO(listOf(VariantUpdate(product.id, emptyList(), supplierRef = "supplier-ref"))),
            product.supplierId, product.seriesUUID, supplier,
        )
        result.failed.shouldBeEmpty()
        result.updated.single().supplierRef shouldBe "supplier-ref"
        agreements.findById(agreement.id)!!.supplierRef shouldBe "supplier-ref"
        agreements.findById(agreement.id)!!.hmsArtNr shouldBe product.hmsArtNr
        catalogs.findById(catalog.id)!!.supplierRef shouldBe "supplier-ref"
        catalogs.findById(catalog.id)!!.hmsArtNr shouldBe product.hmsArtNr
    }

    @Test
    fun `duplicate supplier reference rolls back related writes without stopping the batch`(): Unit = runBlocking {
        val result = service.bulkUpdateVariants(
            BulkVariantUpdateDTO(
                listOf(
                    VariantUpdate(product.id, emptyList(), articleName = "Rejected", supplierRef = other.supplierRef),
                    VariantUpdate(other.id, emptyList(), articleName = "Saved"),
                ),
            ),
            admin,
        )
        result.failed.single().productId shouldBe product.id
        result.failed.single().message shouldBe "Artikkelnummeret finnes allerede på en annen variant"
        result.updated.single().id shouldBe other.id
        products.findById(product.id) shouldBe product
        agreements.findById(agreement.id) shouldBe agreement
        catalogs.findById(catalog.id) shouldBe catalog
        products.findById(other.id)!!.articleName shouldBe "Saved"
    }

    @Test
    fun `a failed product write rolls back its relations and allows other variants to save`(): Unit = runBlocking {
        val result = service.bulkUpdateVariants(
            BulkVariantUpdateDTO(
                listOf(
                    VariantUpdate(product.id, emptyList(), articleName = "Rejected", hmsArtNr = other.hmsArtNr),
                    VariantUpdate(other.id, emptyList(), articleName = "Saved"),
                ),
            ),
            admin,
        )
        result.failed.single().productId shouldBe product.id
        result.updated.single().id shouldBe other.id
        products.findById(product.id) shouldBe product
        catalogs.findById(catalog.id) shouldBe catalog
        agreements.findById(agreement.id) shouldBe agreement
        unrelatedCatalogs.forEach { catalogs.findById(it.id) shouldBe it }
        products.findById(other.id)!!.articleName shouldBe "Saved"
    }
}
