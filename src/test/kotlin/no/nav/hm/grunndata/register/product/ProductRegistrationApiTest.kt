package no.nav.hm.grunndata.register.product

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.AgreementInfo
import no.nav.hm.grunndata.rapid.dto.Attributes
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.MediaSourceType
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.TechData
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.security.LoginClient
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationDTO
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserAttribute
import no.nav.hm.grunndata.register.user.UserRepository
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.Ignore
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.*

@MicronautTest
class ProductRegistrationApiTest(
    private val apiClient: ProductRegistrationApiClient,
    private val loginClient: LoginClient,
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper,
    private val supplierRegistrationService: SupplierRegistrationService,
) {
    private val email = "api@test.test"
    private val password = "api-123"
    private var testSupplier: SupplierRegistrationDTO? = null
    private var testSupplier2: SupplierRegistrationDTO? = null

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @BeforeEach
    fun createUserSupplier() {
        val name1 = "25cfec1d-fc9b-474e-ab3a-7c997fbc8e73"
        val name2 = "ba38e5a7-fce3-46ad-9548-a874d967b2a2"
        val supplierId = UUID.randomUUID()
        val supplierId2 = UUID.randomUUID()
        runBlocking {
            if (supplierRegistrationService.findByName(name1) == null) {
                supplierRegistrationService.save(
                    SupplierRegistrationDTO(
                        id = supplierId,
                        supplierData =
                            SupplierData(
                                address = "address 3",
                                homepage = "https://www.hompage.no",
                                phone = "+47 12345678",
                                email = "supplier3@test.test",
                            ),
                        identifier = name1,
                        name = name1,
                    ),
                )
                supplierRegistrationService.save(
                    SupplierRegistrationDTO(
                        id = supplierId2,
                        supplierData =
                            SupplierData(
                                address = "address 4",
                                homepage = "https://www.hompage.no",
                                phone = "+47 12345678",
                                email = "supplier4@test.test",
                            ),
                        identifier = name2,
                        name = name2,
                    ),
                )
                userRepository.createUser(
                    User(
                        email = email,
                        token = password,
                        name = "User tester",
                        roles = listOf(Roles.ROLE_SUPPLIER),
                        attributes = mapOf(Pair(UserAttribute.SUPPLIER_ID, supplierId.toString())),
                    ),
                )
            }
            testSupplier = supplierRegistrationService.findByName(name1)
            testSupplier2 = supplierRegistrationService.findByName(name2)
        }
    }

    @Test
    fun `fetch product series with variants`() {
        val resp = loginClient.login(UsernamePasswordCredentials(email, password))
        val jwt = resp.getCookie("JWT").get().value
        val seriesUUID = UUID.randomUUID()

        apiClient.createProduct(
            jwt,
            dummyProductRegistrationDTO(
                supplierRef = UUID.randomUUID().toString(),
                supplierId = testSupplier!!.id,
                seriesUUID = seriesUUID,
                articleName = "variant 1",
            ),
        )
        apiClient.createProduct(
            jwt,
            dummyProductRegistrationDTO(
                supplierRef = UUID.randomUUID().toString(),
                supplierId = testSupplier!!.id,
                seriesUUID = seriesUUID,
                articleName = "variant 2",
            ),
        )

        val read = apiClient.readProductSeriesWithVariants(jwt, seriesUUID)
        read.shouldNotBeNull()
        read.createdByUser shouldBe email
        read.variants.size shouldBe 2
    }

    @Test
    fun `fetch product series without variants`() {
        val resp = loginClient.login(UsernamePasswordCredentials(email, password))
        val jwt = resp.getCookie("JWT").get().value
        val seriesUUID = UUID.randomUUID()

        apiClient.createProduct(
            jwt,
            dummyProductRegistrationDTO(
                supplierRef = "not uuid",
                supplierId = testSupplier!!.id,
                seriesUUID = seriesUUID,
            ),
        )

        val read = apiClient.readProductSeriesWithVariants(jwt, seriesUUID)
        read.shouldNotBeNull()
        read.variants.size shouldBe 0
    }

    @Test
    fun `update common data for product series`() {
        val resp = loginClient.login(UsernamePasswordCredentials(email, password))
        val jwt = resp.getCookie("JWT").get().value
        val seriesUUID = UUID.randomUUID()

        apiClient.createProduct(
            jwt,
            dummyProductRegistrationDTO(
                supplierRef = UUID.randomUUID().toString(),
                supplierId = testSupplier!!.id,
                seriesUUID = seriesUUID,
                title = "title",
            ),
        )
        val read = apiClient.readProductSeriesWithVariants(jwt, seriesUUID)
        read.title shouldBe "title"

        apiClient.updateProductSeriesWithVariants(jwt, seriesUUID, read.copy(title = "changed title"))

        val changed = apiClient.readProductSeriesWithVariants(jwt, seriesUUID)
        changed.title shouldBe "changed title"
    }



    @Test
    fun `variants with same supplierId and supplierRef returns error`() {
        val resp = loginClient.login(UsernamePasswordCredentials(email, password))
        val jwt = resp.getCookie("JWT").get().value
        val seriesUUID = UUID.randomUUID()

        val supplierRef = UUID.randomUUID().toString()

        apiClient.createProduct(
            jwt,
            dummyProductRegistrationDTO(
                supplierRef = supplierRef,
                supplierId = testSupplier!!.id,
                seriesUUID = seriesUUID,
                articleName = "variant 1",
            ),
        )

        assertThrows<Exception> {
            apiClient.createProduct(
                jwt,
                dummyProductRegistrationDTO(
                    supplierRef = supplierRef,
                    supplierId = testSupplier!!.id,
                    seriesUUID = seriesUUID,
                    articleName = "variant 2",
                ),
            )
        }
    }

    @Test
    fun apiTest() {
        val resp = loginClient.login(UsernamePasswordCredentials(email, password))
        val jwt = resp.getCookie("JWT").get().value
        val seriesUUID = UUID.randomUUID()
        val productData =
            ProductData(
                attributes =
                    Attributes(
                        shortdescription = "En kort beskrivelse av produktet",
                        text = "En lang beskrivelse av produktet",
                    ),
                accessory = false,
                sparePart = false,
                techData = listOf(TechData(key = "maksvekt", unit = "kg", value = "120")),
                media =
                    setOf(
                        MediaInfoDTO(
                            uri = "123.jpg",
                            text = "bilde av produktet",
                            source = MediaSourceType.EXTERNALURL,
                            sourceUri = "https://ekstern.url/123.jpg",
                        ),
                    ),
            )

        val registration =
            ProductRegistrationDTO(
                seriesId = "apitest-series-123",
                seriesUUID = seriesUUID,
                title = "apitest-produkt 1",
                articleName = "Dette er produkt 1 med og med",
                id = UUID.randomUUID(),
                isoCategory = "12001314",
                supplierId = testSupplier!!.id,
                hmsArtNr = "apitest-111",
                supplierRef = "apitest-eksternref-111",
                draftStatus = DraftStatus.DRAFT,
                adminStatus = AdminStatus.PENDING,
                registrationStatus = RegistrationStatus.ACTIVE,
                message = "Melding til leverandør",
                adminInfo = null,
                createdByAdmin = false,
                expired = null,
                published = null,
                updatedByUser = email,
                createdByUser = email,
                productData = productData,
                version = 1,
                createdBy = REGISTER,
                updatedBy = REGISTER,
            )
        val created = apiClient.createProduct(jwt, registration)
        created.shouldNotBeNull()

        // create another one

        val productData2 =
            ProductData(
                attributes =
                    Attributes(
                        shortdescription = "En kort beskrivelse av produktet",
                        text = "En lang beskrivelse av produktet",
                    ),
                accessory = false,
                sparePart = false,
                techData = listOf(TechData(key = "maksvekt", unit = "kg", value = "120")),
                media =
                    setOf(
                        MediaInfoDTO(
                            uri = "123.jpg",
                            text = "bilde av produktet",
                            source = MediaSourceType.EXTERNALURL,
                            sourceUri = "https://ekstern.url/123.jpg",
                        ),
                    ),
            )

        val registration2 =
            ProductRegistrationDTO(
                title = "apitest-produkt 2",
                articleName = "en veldig fin tittel med og med",
                id = UUID.randomUUID(),
                seriesId = "apitest-series-123",
                seriesUUID = seriesUUID,
                isoCategory = "12001314",
                supplierId = testSupplier!!.id,
                hmsArtNr = "apitest-222",
                supplierRef = "eksternref-222",
                draftStatus = DraftStatus.DRAFT,
                adminStatus = AdminStatus.PENDING,
                registrationStatus = RegistrationStatus.ACTIVE,
                message = "Melding til leverandør",
                adminInfo = null,
                createdByAdmin = false,
                expired = null,
                published = null,
                updatedByUser = email,
                createdByUser = email,
                productData = productData2,
                version = 1,
                createdBy = REGISTER,
                updatedBy = REGISTER,
            )

        val created2 = apiClient.createProduct(jwt, registration2)
        created2.shouldNotBeNull()

        println(objectMapper.writeValueAsString(apiClient.findSeriesGroup(jwt, 20, 0, null)))

        val read = apiClient.readProduct(jwt, created.id)
        read.shouldNotBeNull()
        read.createdByUser shouldBe email

        val updated =
            apiClient.updateProduct(
                jwt,
                read.id,
                read.copy(title = "Changed title", articleName = "Changed articlename", draftStatus = DraftStatus.DONE),
            )
        updated.shouldNotBeNull()

        val draftStatusChange = apiClient.updateProduct(jwt, updated.id, updated.copy(draftStatus = DraftStatus.DRAFT))
        draftStatusChange.shouldNotBeNull()
        draftStatusChange.draftStatus shouldBe DraftStatus.DRAFT // not APPROVED yet allowed to change status

        val deleted = apiClient.deleteProduct(jwt, updated.id)
        deleted.shouldNotBeNull()
        deleted.registrationStatus shouldBe RegistrationStatus.DELETED

        val page = apiClient.findProducts(jwt, null, "%apitest-produkt%", 30, 1, "created,asc")
        page.totalSize shouldBe 1

        val page2 = apiClient.findProducts(jwt, "apitest-222", null, 30, 1, "created,asc")
        page2.totalSize shouldBe 1

        val updatedVersion = apiClient.readProduct(jwt, updated.id)
        updatedVersion.version!! shouldBeGreaterThan 0
        updatedVersion.updatedByUser shouldBe email

        // should not be allowed to create a product of another supplier
        val productData3 =
            ProductData(
                attributes =
                    Attributes(
                        shortdescription = "En kort beskrivelse av produktet",
                        text = "En lang beskrivelse av produktet",
                    ),
                accessory = false,
                sparePart = false,
                techData = listOf(TechData(key = "maksvekt", unit = "kg", value = "120")),
                media =
                    setOf(
                        MediaInfoDTO(
                            uri = "123.jpg",
                            text = "bilde av produktet",
                            source = MediaSourceType.EXTERNALURL,
                            sourceUri = "https://ekstern.url/123.jpg",
                        ),
                    ),
            )
        val registration3 =
            ProductRegistrationDTO(
                id = UUID.randomUUID(),
                seriesId = "apitest-series-123",
                seriesUUID = UUID.randomUUID(),
                isoCategory = "12001314",
                supplierId = testSupplier2!!.id,
                title = "apitest-produkt 3",
                articleName = "Dette er produkt 1 med og med",
                hmsArtNr = "apitest-333",
                supplierRef = "apitest-eksternref-333",
                draftStatus = DraftStatus.DRAFT,
                adminStatus = AdminStatus.PENDING,
                registrationStatus = RegistrationStatus.ACTIVE,
                message = "Melding til leverandør",
                adminInfo = null,
                createdByAdmin = false,
                expired = null,
                published = null,
                updatedByUser = email,
                createdByUser = email,
                productData = productData3,
                version = 1,
                createdBy = REGISTER,
                updatedBy = REGISTER,
            )
        runCatching {
            apiClient.createProduct(jwt, registration3)
        }.isFailure shouldBe true
    }

    private fun dummyProductRegistrationDTO(
        id: UUID = UUID.randomUUID(),
        supplierId: UUID = UUID.randomUUID(),
        supplierRef: String = UUID.randomUUID().toString(),
        hmsArtNr: String? = null,
        seriesUUID: UUID = UUID.randomUUID(),
        seriesId: String = seriesUUID.toString(),
        isoCategory: String = "dummyIso",
        title: String = "dummyTitle",
        articleName: String = "dummyArticleName",
        draftStatus: DraftStatus = DraftStatus.DRAFT,
        adminStatus: AdminStatus = AdminStatus.PENDING,
        registrationStatus: RegistrationStatus = RegistrationStatus.ACTIVE,
        message: String? = null,
        adminInfo: AdminInfo? = null,
        published: LocalDateTime? = null,
        expired: LocalDateTime? = null,
        updatedByUser: String = "dummyUser",
        createdByUser: String = "dummyUser",
        createdBy: String = REGISTER,
        updatedBy: String = REGISTER,
        createdByAdmin: Boolean = false,
        productData: ProductData =
            ProductData(
                attributes =
                    Attributes(
                        shortdescription = "En kort beskrivelse av produktet",
                        text = "En lang beskrivelse av produktet",
                    ),
                accessory = false,
                sparePart = false,
            ),
        agreements: List<AgreementInfo> = emptyList(),
        version: Long? = 1,
    ): ProductRegistrationDTO {
        return ProductRegistrationDTO(
            seriesId = seriesId,
            seriesUUID = seriesUUID,
            title = title,
            articleName = articleName,
            id = id,
            isoCategory = isoCategory,
            supplierId = supplierId,
            hmsArtNr = hmsArtNr,
            supplierRef = supplierRef,
            draftStatus = draftStatus,
            adminStatus = adminStatus,
            registrationStatus = registrationStatus,
            message = message,
            adminInfo = adminInfo,
            createdByAdmin = createdByAdmin,
            expired = expired,
            published = published,
            updatedByUser = updatedByUser,
            createdByUser = createdByUser,
            productData = productData,
            version = version,
            createdBy = createdBy,
            updatedBy = updatedBy,
            agreements = agreements,
        )
    }
}
