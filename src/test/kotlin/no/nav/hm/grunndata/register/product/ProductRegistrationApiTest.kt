package no.nav.hm.grunndata.register.product

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.AgreementInfo
import no.nav.hm.grunndata.rapid.dto.Attributes
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.MediaSourceType
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.rapid.dto.TechData
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.security.LoginClient
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.series.SeriesAttributesDTO
import no.nav.hm.grunndata.register.series.SeriesDataDTO
import no.nav.hm.grunndata.register.series.SeriesRegistration
import no.nav.hm.grunndata.register.series.SeriesRegistrationRepository
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationDTO
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserAttribute
import no.nav.hm.grunndata.register.user.UserRepository
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@MicronautTest
class ProductRegistrationApiTest(
    private val apiClient: ProductRegistrationApiClient,
    private val loginClient: LoginClient,
    private val userRepository: UserRepository,
    private val seriesRepository: SeriesRegistrationRepository,
    private val objectMapper: ObjectMapper,
    private val supplierRegistrationService: SupplierRegistrationService,
) {
    private val email = "api@test.test"
    private val password = "api-123"
    private var testSupplier: SupplierRegistrationDTO? = null
    private var testSupplier2: SupplierRegistrationDTO? = null
    private var seriesRegistrationSupplier1: SeriesRegistration? = null
    private var seriesRegistrationSupplier2: SeriesRegistration? = null

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @BeforeEach
    fun createUserSupplierAndSeries() {
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

            seriesRegistrationSupplier1 =
                seriesRepository.save(
                    SeriesRegistration(
                        id = UUID.randomUUID(),
                        draftStatus = DraftStatus.DONE,
                        supplierId = supplierId,
                        identifier = "apitest-series-123",
                        title = "apitest-series",
                        text = "apitest-series",
                        isoCategory = "12001314",
                        status = SeriesStatus.ACTIVE,
                        adminStatus = AdminStatus.APPROVED,
                        seriesData =
                            SeriesDataDTO(
                                media = emptySet(),
                                attributes = SeriesAttributesDTO(keywords = listOf("keyword1", "keyword2")),
                            ),
                        createdBy = REGISTER,
                        updatedBy = REGISTER,
                        updatedByUser = email,
                        createdByUser = email,
                        createdByAdmin = false,
                        version = 1,
                    ),
                )

            seriesRegistrationSupplier2 =
                seriesRepository.save(
                    SeriesRegistration(
                        id = UUID.randomUUID(),
                        draftStatus = DraftStatus.DONE,
                        supplierId = supplierId2,
                        identifier = "apitest-series-123",
                        title = "apitest-series",
                        text = "apitest-series",
                        isoCategory = "12001314",
                        status = SeriesStatus.ACTIVE,
                        adminStatus = AdminStatus.APPROVED,
                        seriesData =
                            SeriesDataDTO(
                                media = emptySet(),
                                attributes = SeriesAttributesDTO(keywords = listOf("keyword1", "keyword2")),
                            ),
                        createdBy = REGISTER,
                        updatedBy = REGISTER,
                        updatedByUser = email,
                        createdByUser = email,
                        createdByAdmin = false,
                        version = 1,
                    ),
                )
        }
    }

    @Test
    fun `variants with same supplierId and supplierRef returns error`() {
        val resp = loginClient.login(UsernamePasswordCredentials(email, password))
        val jwt = resp.getCookie("JWT").get().value

        val supplierRef = UUID.randomUUID().toString()

        apiClient.createDraftWith(
            jwt,
            seriesRegistrationSupplier1!!.id,
            testSupplier!!.id,
            dummyDraftWith(
                supplierRef = supplierRef,
                articleName = "variant 1",
            ),
        )

        assertThrows<Exception> {
            apiClient.createDraftWith(
                jwt,
                seriesRegistrationSupplier1!!.id,
                testSupplier!!.id,
                dummyDraftWith(
                    articleName = "variant 2",
                    supplierRef = supplierRef,
                ),
            )
        }
    }

    @Test
    fun `delete variant`() {
        val resp = loginClient.login(UsernamePasswordCredentials(email, password))
        val jwt = resp.getCookie("JWT").get().value

        val variant = apiClient.createDraftWith(
            jwt,
            seriesRegistrationSupplier1!!.id,
            testSupplier!!.id,
            dummyDraftWith(
                supplierRef = UUID.randomUUID().toString(),
            ),
        )

        apiClient.deleteDraftVariants(jwt, listOf(variant.id))

        val variants = apiClient.findBySeriesUUIDAndSupplierId(
            jwt = jwt,
            seriesUUID = variant.seriesUUID
        )

        variants.size shouldBe 0
    }

    @Test
    fun apiTest() {
        val resp = loginClient.login(UsernamePasswordCredentials(email, password))
        val jwt = resp.getCookie("JWT").get().value
        val seriesUUID = seriesRegistrationSupplier1!!.id
        val productData =
            ProductData(
                attributes =
                    Attributes(
                        shortdescription = "En kort beskrivelse av produktet",
                        text = "En lang beskrivelse av produktet",
                    ),
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

        val draft1 =
            apiClient.createDraftWith(
                jwt,
                seriesUUID,
                testSupplier!!.id,
                dummyDraftWith(
                    supplierRef = "apitest-eksternref-111",
                    articleName = "Dette er produkt 1 med og med",
                ),
            )

        val created =
            apiClient.updateProduct(
                jwt,
                draft1.id,
                draft1.copy(productData = productData, hmsArtNr = "apitest-222"),
            )
        created.shouldNotBeNull()

        // create another one

        val productData2 =
            ProductData(
                attributes =
                    Attributes(
                        shortdescription = "En kort beskrivelse av produktet",
                        text = "En lang beskrivelse av produktet",
                    ),
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

        val draft2 =
            apiClient.createDraftWith(
                jwt,
                seriesUUID,
                testSupplier!!.id,
                dummyDraftWith(
                    supplierRef = "eksternref-222",
                    articleName = "en veldig fin tittel med og med",
                ),
            )
        val created2 = apiClient.updateProduct(jwt, draft2.id, draft2.copy(productData = productData2))
        created2.shouldNotBeNull()

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

        val page2 =
            apiClient.findProducts(jwt = jwt, hmsArtNr = "apitest-222", size = 30, page = 1, sort = "created,asc")
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

        runCatching {
            apiClient.createDraftWith(
                jwt,
                seriesRegistrationSupplier2!!.id,
                testSupplier2!!.id,
                dummyDraftWith(
                    supplierRef = "apitest-eksternref-333",
                    articleName = "Dette er produkt 1 med og med",
                ),
            )
        }.isFailure shouldBe true
    }

    private fun dummyDraftWith(
        articleName: String = "dummyArticleName",
        supplierRef: String = UUID.randomUUID().toString(),
    ): DraftVariantDTO {
        return DraftVariantDTO(
            supplierRef = supplierRef,
            articleName = articleName,
        )
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
