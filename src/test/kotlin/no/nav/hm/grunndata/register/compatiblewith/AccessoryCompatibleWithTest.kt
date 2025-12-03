package no.nav.hm.grunndata.register.compatiblewith


import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.toUUID
import no.nav.hm.grunndata.register.catalog.CatalogImport
import no.nav.hm.grunndata.register.catalog.CatalogImportRepository
import no.nav.hm.grunndata.register.part.CompatibleWithDTO
import no.nav.hm.grunndata.register.product.ProductData
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import no.nav.hm.grunndata.register.security.LoginClient
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.series.SeriesDataDTO
import no.nav.hm.grunndata.register.series.SeriesRegistration
import no.nav.hm.grunndata.register.series.SeriesRegistrationRepository
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserRepository
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@MicronautTest
class AccessoryCompatibleWithTest(
    private val userRepository: UserRepository,
    private val catalogImportRepository: CatalogImportRepository,
    private val seriesRegistrationRepository: SeriesRegistrationRepository,
    private val loginClient: LoginClient,
    private val productRegistrationRepository: ProductRegistrationRepository,
    private val accessoryCompatibleWithApiClient: AccessoryCompatibleWithApiClient
) {

    val userEmail = "accessory1@test.test"
    val password = "test123"
    val productId = UUID.randomUUID()
    val supplierId = UUID.randomUUID()
    val agreementId = UUID.randomUUID()
    val aSeriesId = UUID.randomUUID()
    val mSeriesId = UUID.randomUUID()

    @MockBean(CompatibleAIFinder::class)
    fun mockCompatibleFinder(): CompatibleAIFinder = mockk<CompatibleAIFinder>(relaxed = true).apply {
        coEvery {
            findCompatibleProducts(any(), any())
        } coAnswers {
            listOf(HmsNr("229392"))
        }
    }

    init {
        runBlocking {
            val hmsUser = userRepository.createUser(
                User(
                    email = userEmail, token = password, name = "HMS tester", roles = listOf(Roles.ROLE_HMS)
                )
            )
            val product = productRegistrationRepository.save(
                ProductRegistration(
                    id = productId,
                    title = "Test accessory product",
                    hmsArtNr = "229391",
                    seriesUUID = aSeriesId,
                    articleName = "Test article",
                    supplierId = supplierId,
                    supplierRef = "Supplier ref1",
                    productData = ProductData(),
                    accessory = true,
                    mainProduct = false,
                    created = LocalDateTime.now()
                )
            )
            val series = seriesRegistrationRepository.save(
                SeriesRegistration(
                    id = aSeriesId,
                    supplierId = supplierId,
                    identifier = aSeriesId.toString(),
                    title = "Test series",
                    text = "Test series text",
                    isoCategory = "ISO1234",
                    seriesData = SeriesDataDTO()
                )
            )
            val catalogAccessory = catalogImportRepository.save(CatalogImport(
                id = UUID.randomUUID(),
                orderRef = "123456",
                hmsArtNr = "229391",
                created = LocalDateTime.now(),
                updated = LocalDateTime.now(),
                agreementAction = "",
                iso = "",
                title = "Test accessory product 1",
                supplierId = supplierId,
                supplierRef = "Supplier ref1",
                reference = "Test reference 1",
                postNr = "1",
                dateFrom = LocalDate.now(),
                dateTo = LocalDate.now().plusDays(1),
                articleAction = "",
                articleType = "",
                functionalChange = "",
                forChildren = "",
                supplierName = "",
                supplierCity = "",
                mainProduct = false,
                sparePart = false,
                accessory = true,
                agreementId = agreementId
            ))
            val mainProduct = productRegistrationRepository.save(
                ProductRegistration(
                    id = UUID.randomUUID(),
                    title = "Test main product",
                    hmsArtNr = "229392",
                    seriesUUID = mSeriesId,
                    articleName = "Test main article",
                    supplierId = supplierId,
                    supplierRef = "Supplier ref2",
                    productData = ProductData(),
                    mainProduct = true,
                    accessory = false,
                    created = LocalDateTime.now()
                )
            )
            val mainSeries = seriesRegistrationRepository.save(
                SeriesRegistration(
                    id = mSeriesId,
                    supplierId = supplierId,
                    identifier = mSeriesId.toString(),
                    title = "Test main series",
                    text = "Test main series text",
                    isoCategory = "ISO1234",
                    seriesData = SeriesDataDTO()
                )
            )
            val mainCatalogImport = catalogImportRepository.save(CatalogImport(
                id = UUID.randomUUID(),
                orderRef = "123456",
                hmsArtNr = "229392",
                created = LocalDateTime.now(),
                updated = LocalDateTime.now(),
                agreementAction = "",
                iso = "",
                title = "Test accessory product",
                supplierId = supplierId,
                supplierRef = "Supplier ref2",
                reference = "Test reference",
                postNr = "1",
                dateFrom = LocalDate.now(),
                dateTo = LocalDate.now().plusDays(1),
                articleAction = "",
                articleType = "",
                functionalChange = "",
                forChildren = "",
                supplierName = "",
                supplierCity = "",
                mainProduct = true,
                sparePart = false,
                accessory = false,
                agreementId = agreementId
            ))


        }
    }

    @Test
    fun testAccessoryCompatibleWith() {
        runBlocking {
            val jwtHms = loginClient.login(UsernamePasswordCredentials(userEmail, password))
                .getCookie("JWT").get().value

            val compatibleProductResults =
                accessoryCompatibleWithApiClient.findCompatibleWithProductsVariants(jwtHms, "229391")
            compatibleProductResults.shouldNotBeNull()
            compatibleProductResults.size shouldBe 1
            val item = compatibleProductResults[0]
            val compatibleWithDTO = CompatibleWithDTO(
                seriesIds = setOf(item.seriesId.toUUID()),
                productIds = setOf(item.productId.toUUID())
            )
            val connectedProduct = accessoryCompatibleWithApiClient.connectProductAndVariants(jwtHms, compatibleWithDTO, productId)
            connectedProduct.shouldNotBeNull()
            connectedProduct.articleName shouldBe "Test article"
            connectedProduct.productData.attributes.compatibleWith.shouldNotBeNull()
            connectedProduct.productData.attributes.compatibleWith!!.seriesIds.size shouldBe 1
            connectedProduct.productData.attributes.compatibleWith!!.productIds.size shouldBe 1
        }
    }
}