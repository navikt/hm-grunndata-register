package no.nav.hm.grunndata.register.product

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.TechData
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.event.EventItemRepository
import no.nav.hm.grunndata.register.event.EventItemType
import no.nav.hm.grunndata.register.security.LoginClient
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.series.SeriesCommonControllerApiClient
import no.nav.hm.grunndata.register.series.SeriesDataDTO
import no.nav.hm.grunndata.register.series.SeriesRegistration
import no.nav.hm.grunndata.register.series.SeriesRegistrationRepository
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationDTO
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.grunndata.register.supplier.SupplierRepository
import no.nav.hm.grunndata.register.techlabel.TechLabelService
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserAttribute
import no.nav.hm.grunndata.register.user.UserRepository
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

@MicronautTest(transactional = false)
class ProductRegistrationBulkVariantSupplierApiTest(
    private val apiClient: ProductRegistrationApiClient,
    private val loginClient: LoginClient,
    private val userRepository: UserRepository,
    private val supplierService: SupplierRegistrationService,
    private val seriesRepository: SeriesRegistrationRepository,
    private val seriesApiClient: SeriesCommonControllerApiClient,
    private val productRepository: ProductRegistrationRepository,
    private val eventRepository: EventItemRepository,
    private val supplierRepository: SupplierRepository,
) {
    private val supplierId = UUID.randomUUID()
    private val otherSupplierId = UUID.randomUUID()
    private val seriesId = UUID.randomUUID()
    private val otherSeriesId = UUID.randomUUID()
    private val foreignSeriesId = UUID.randomUUID()
    private val email = "$supplierId@test.test"
    private val password = "supplier-test-password"
    private lateinit var ownVariants: List<ProductRegistration>
    private lateinit var otherSeriesVariant: ProductRegistration
    private lateinit var foreignVariant: ProductRegistration

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @MockBean(TechLabelService::class)
    fun techLabelService(): TechLabelService = mockk<TechLabelService>().apply {
        every { fetchLabelsByIsoCode(any()) } returns emptyList()
        every { fetchLabelsByIsoCode22(any()) } returns emptyList()
    }

    @BeforeEach
    fun createFixtures() = runBlocking {
        listOf(supplierId, otherSupplierId).forEach { id ->
            supplierService.save(
                SupplierRegistrationDTO(
                    id = id,
                    name = "Supplier $id",
                    identifier = id.toString(),
                    supplierData = SupplierData(
                        email = "$id@test.test", address = "Test address",
                        homepage = "https://example.test", phone = "12345678",
                    ),
                )
            )
        }
        listOf(seriesId to supplierId, otherSeriesId to supplierId, foreignSeriesId to otherSupplierId)
            .forEach { (id, owner) ->
                seriesRepository.save(
                    SeriesRegistration(
                        id = id, supplierId = owner, title = "Series $id", text = "",
                        isoCategory = "04360901", isoCategory22 = "04360902", seriesData = SeriesDataDTO(),
                        updated = LocalDateTime.now().minusDays(1),
                        updatedByUser = "previous-editor", updatedBy = "IMPORT",
                    )
                )
            }
        userRepository.createUser(
            User(
                email = email, token = password, name = "Supplier",
                roles = listOf(Roles.ROLE_SUPPLIER),
                attributes = mapOf(UserAttribute.SUPPLIER_ID to supplierId.toString()),
            )
        )
        ownVariants = listOf(createVariant(seriesId, supplierId), createVariant(seriesId, supplierId))
        otherSeriesVariant = createVariant(otherSeriesId, supplierId)
        foreignVariant = createVariant(foreignSeriesId, otherSupplierId)
    }

    private suspend fun createVariant(series: UUID, owner: UUID): ProductRegistration {
        val id = UUID.randomUUID()
        productRepository.save(
            ProductRegistration(
                id = id, seriesUUID = series, supplierId = owner, supplierRef = id.toString(),
                articleName = "Variant $id", hmsArtNr = null,
                draftStatus = DraftStatus.DONE, adminStatus = AdminStatus.APPROVED,
                published = LocalDateTime.now().minusDays(1),
                productData = ProductData(techData = listOf(TechData(key = "Vekt", value = "1", unit = "kg"))),
            )
        )
        return productRepository.findById(id)!!
    }

    @AfterEach
    fun removeFixtures() = runBlocking {
        val products = listOf(seriesId, otherSeriesId, foreignSeriesId)
            .flatMap { productRepository.findBySeriesUUIDAndSupplierId(it, supplierId) } +
            productRepository.findBySeriesUUIDAndSupplierId(foreignSeriesId, otherSupplierId)
        val fixtureIds = products.map { it.id } + listOf(seriesId, otherSeriesId, foreignSeriesId, supplierId, otherSupplierId)
        eventRepository.findAll().toList().filter { it.oid in fixtureIds }.forEach {
            eventRepository.deleteById(it.eventId)
        }
        products.forEach { productRepository.deleteById(it.id) }
        listOf(seriesId, otherSeriesId, foreignSeriesId).forEach { seriesRepository.deleteById(it) }
        (listOf(email, "mixed-$supplierId@test.test") + (0..2).map { "$it-$supplierId@test.test" }).forEach {
            userRepository.findByEmailIgnoreCase(it)?.let { user -> userRepository.deleteById(user.id) }
        }
        listOf(supplierId, otherSupplierId).forEach { supplierRepository.deleteById(it) }
    }

    private suspend fun productEventIds(): List<UUID> =
        eventRepository.findAll().toList().filter { it.type == EventItemType.PRODUCT }.map { it.oid }

    private fun login(userEmail: String = email): String =
        loginClient.login(UsernamePasswordCredentials(userEmail, password)).getCookie("JWT").get().value

    private fun update(id: UUID, value: String = " 20 "): VariantUpdate = VariantUpdate(
        id,
        listOf(
            ExtendedTechDataDTO(
                key = "Vekt", value = value, unit = "kg", type = TechDataType.NUMBER,
                definition = null, required = false, options = emptySet(),
            )
        ),
    )

    @Test
    fun `supplier bulk updates only owned variants in the requested series`(): Unit = runBlocking {
        val unknownId = UUID.randomUUID()
        val seriesBefore = seriesRepository.findById(seriesId)!!
        val otherSeriesBefore = seriesRepository.findById(otherSeriesId)
        val foreignSeriesBefore = seriesRepository.findById(foreignSeriesId)
        val jwt = login()
        seriesApiClient.findSeriesByUpdatedByUser(jwt, email).content.shouldBeEmpty()
        val result = apiClient.bulkUpdateVariants(
            jwt, seriesId,
            BulkVariantUpdateDTO(
                (ownVariants.map { update(it.id) } +
                    listOf(update(otherSeriesVariant.id), update(foreignVariant.id), update(unknownId)))
                    .map { it.copy(articleName = "  Nytt variantnavn  ") }
            ),
            "v22",
        )

        result.updated.map { it.id }.toSet() shouldBe ownVariants.map { it.id }.toSet()
        result.failed.map { it.productId }.toSet() shouldBe setOf(otherSeriesVariant.id, foreignVariant.id, unknownId)
        result.failed.map { it.message }.distinct().size shouldBe 1
        val seriesAfter = seriesRepository.findById(seriesId)!!
        seriesAfter.updated.isAfter(seriesBefore.updated) shouldBe true
        seriesAfter.updatedByUser shouldBe email
        seriesAfter.updatedBy shouldBe REGISTER
        seriesRepository.findById(otherSeriesId) shouldBe otherSeriesBefore
        seriesRepository.findById(foreignSeriesId) shouldBe foreignSeriesBefore
        seriesApiClient.findSeriesByUpdatedByUser(jwt, email).content.map { it.id } shouldBe listOf(seriesId)
        ownVariants.forEach { original ->
            val saved = productRepository.findById(original.id)!!
            saved.productData.techData.single().value shouldBe "20"
            saved.articleName shouldBe "Nytt variantnavn"
            saved.draftStatus shouldBe original.draftStatus
            saved.adminStatus shouldBe original.adminStatus
            saved.published shouldBe original.published
            saved.updatedByUser shouldBe email
            productEventIds().count { it == original.id } shouldBe 1
        }
        listOf(otherSeriesVariant, foreignVariant).forEach { original ->
            productRepository.findById(original.id) shouldBe original
            productEventIds().count { it == original.id } shouldBe 0
        }

        listOf(UUID.randomUUID(), foreignSeriesId).forEach { wrongSeries ->
            val rejected = apiClient.bulkUpdateVariants(
                login(), wrongSeries, BulkVariantUpdateDTO(listOf(update(ownVariants.first().id, "30"))),
            )
            rejected.updated.shouldBeEmpty()
            rejected.failed.single().message shouldBe result.failed.first().message
            productRepository.findById(ownVariants.first().id)!!.productData.techData.single().value shouldBe "20"
            productEventIds().count { it == ownVariants.first().id } shouldBe 1
        }
        seriesRepository.findById(seriesId) shouldBe seriesAfter
        seriesRepository.findById(foreignSeriesId) shouldBe foreignSeriesBefore
    }

    @Test
    fun `supplier saves names and properties together and rejects invalid names without saving either`(): Unit = runBlocking {
        val original = ownVariants.first()
        listOf("", "   ", "x".repeat(1025)).forEach { invalidName ->
            val result = apiClient.bulkUpdateVariants(
                login(), seriesId,
                BulkVariantUpdateDTO(listOf(update(original.id).copy(articleName = invalidName))),
            )
            result.updated.shouldBeEmpty()
            result.failed.single().productId shouldBe original.id
            productRepository.findById(original.id) shouldBe original
        }
        val partial = apiClient.bulkUpdateVariants(
            login(), seriesId,
            BulkVariantUpdateDTO(
                listOf(
                    update(original.id).copy(articleName = " "),
                    update(ownVariants.last().id).copy(articleName = "Lagret variantnavn"),
                )
            ),
        )
        partial.failed.single().productId shouldBe original.id
        partial.updated.single().articleName shouldBe "Lagret variantnavn"
        productRepository.findById(original.id) shouldBe original
        val result = apiClient.bulkUpdateVariants(
            login(), seriesId,
            BulkVariantUpdateDTO(
                listOf(
                    update(original.id).copy(articleName = "  ${"x".repeat(1024)}  "),
                    update(ownVariants.last().id),
                )
            ),
        )
        result.failed.shouldBeEmpty()
        val saved = productRepository.findById(original.id)!!
        saved.articleName shouldBe "x".repeat(1024)
        saved.productData.techData.single().value shouldBe "20"
        productEventIds().count { it == original.id } shouldBe 1
        productRepository.findById(ownVariants.last().id)!!.articleName shouldBe "Lagret variantnavn"
    }

    @Test
    fun `supplier cannot submit HMS numbers even when unchanged or blank`(): Unit = runBlocking {
        val original = productRepository.update(ownVariants.first().copy(hmsArtNr = "123456"))
        val seriesBefore = seriesRepository.findById(seriesId)
        listOf("123456", "654321", "", "   ").forEach { hmsArtNr ->
            val result = apiClient.bulkUpdateVariants(
                login(), seriesId,
                BulkVariantUpdateDTO(
                    listOf(update(original.id).copy(articleName = "Unauthorized", hmsArtNr = hmsArtNr)),
                ),
            )
            result.updated.shouldBeEmpty()
            result.failed.single().productId shouldBe original.id
            productRepository.findById(original.id) shouldBe original
        }
        seriesRepository.findById(seriesId) shouldBe seriesBefore
        productEventIds().filter { it == original.id }.shouldBeEmpty()

        val partial = apiClient.bulkUpdateVariants(
            login(), seriesId,
            BulkVariantUpdateDTO(
                listOf(
                    update(original.id).copy(hmsArtNr = ""),
                    update(ownVariants.last().id).copy(articleName = "Allowed"),
                ),
            ),
        )
        partial.failed.single().productId shouldBe original.id
        partial.updated.single().articleName shouldBe "Allowed"
        productRepository.findById(original.id) shouldBe original
    }

    @Test
    fun `successful supplier batch refreshes parent series audit fields`(): Unit = runBlocking {
        val before = seriesRepository.findById(seriesId)!!

        val result = apiClient.bulkUpdateVariants(
            login(), seriesId, BulkVariantUpdateDTO(ownVariants.map { update(it.id) }),
        )

        result.failed.shouldBeEmpty()
        result.updated.size shouldBe ownVariants.size
        val after = seriesRepository.findById(seriesId)!!
        after.updated.isAfter(before.updated) shouldBe true
        after.updatedByUser shouldBe email
        after.updatedBy shouldBe REGISTER
    }

    @Test
    fun `supplier route enforces ownership and series even with admin role`(): Unit = runBlocking {
        val seriesBefore = seriesRepository.findByIdIn(listOf(seriesId, otherSeriesId, foreignSeriesId))
        val mixedEmail = "mixed-$supplierId@test.test"
        userRepository.createUser(
            User(
                email = mixedEmail, token = password, name = "Mixed roles",
                roles = listOf(Roles.ROLE_ADMIN, Roles.ROLE_SUPPLIER),
                attributes = mapOf(UserAttribute.SUPPLIER_ID to supplierId.toString()),
            )
        )
        val result = apiClient.bulkUpdateVariants(
            login(mixedEmail), seriesId,
            BulkVariantUpdateDTO(listOf(update(otherSeriesVariant.id), update(foreignVariant.id))),
        )
        result.updated.shouldBeEmpty()
        result.failed.size shouldBe 2
        listOf(otherSeriesVariant, foreignVariant).forEach {
            productRepository.findById(it.id) shouldBe it
        }
        productEventIds().filter { it in listOf(otherSeriesVariant.id, foreignVariant.id) }.shouldBeEmpty()
        seriesRepository.findByIdIn(listOf(seriesId, otherSeriesId, foreignSeriesId)) shouldBe seriesBefore
    }

    @Test
    fun `supplier route rejects missing identity invalid identity and missing supplier role`(): Unit = runBlocking {
        val payload = BulkVariantUpdateDTO(ownVariants.map { update(it.id) })
        listOf(
            listOf(Roles.ROLE_SUPPLIER) to emptyMap(),
            listOf(Roles.ROLE_SUPPLIER) to mapOf(UserAttribute.SUPPLIER_ID to "invalid"),
            listOf(Roles.ROLE_ADMIN) to mapOf(UserAttribute.SUPPLIER_ID to supplierId.toString()),
        ).forEachIndexed { index, (roles, attributes) ->
            val userEmail = "$index-$supplierId@test.test"
            userRepository.createUser(
                User(email = userEmail, token = password, name = "Rejected user", roles = roles, attributes = attributes)
            )
            shouldThrow<HttpClientResponseException> {
                apiClient.bulkUpdateVariants(login(userEmail), seriesId, payload)
            }.status shouldBe HttpStatus.FORBIDDEN
        }
        shouldThrow<HttpClientResponseException> {
            apiClient.bulkUpdateVariants(null, seriesId, payload)
        }.status shouldBe HttpStatus.UNAUTHORIZED
        ownVariants.forEach { productRepository.findById(it.id) shouldBe it }
        productEventIds().filter { it in ownVariants.map { variant -> variant.id } }.shouldBeEmpty()
    }
}
