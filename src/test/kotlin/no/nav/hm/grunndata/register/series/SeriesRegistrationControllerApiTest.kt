package no.nav.hm.grunndata.register.series

import io.kotest.common.runBlocking
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.Attributes
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.MediaSourceType
import no.nav.hm.grunndata.rapid.dto.MediaType
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.rapid.dto.TechData
import no.nav.hm.grunndata.register.product.DraftVariantDTO
import no.nav.hm.grunndata.register.product.MediaInfoDTO
import no.nav.hm.grunndata.register.product.ProductData
import no.nav.hm.grunndata.register.product.ProductRegistrationApiClient
import no.nav.hm.grunndata.register.security.LoginClient
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistration
import no.nav.hm.grunndata.register.supplier.SupplierRepository
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserAttribute
import no.nav.hm.grunndata.register.user.UserRepository
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.UUID

@MicronautTest
class SeriesRegistrationControllerApiTest(
    private val apiClient: SeriesControllerApiClient,
    private val apiAdminClient: SeriesAdminControllerApiClient,
    private val productApiClient: ProductRegistrationApiClient,
    private val loginClient: LoginClient,
    private val userRepository: UserRepository,
    private val supplierRegistrationRepository: SupplierRepository,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesRegistrationControllerTest::class.java)
    }

    private val email = "series-tester@test.test"
    private val emailAdmin = "series-admin@test.test"
    private val password = "api-123"
    private var testSupplier: SupplierRegistration? = null

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @BeforeEach
    fun createUserSupplier() {
        runBlocking {
            LOG.info("Creating user and supplier")
            testSupplier =
                supplierRegistrationRepository.save(
                    SupplierRegistration(
                        id = UUID.randomUUID(),
                        supplierData =
                            SupplierData(
                                address = "address 3",
                                homepage = "https://www.hompage.no",
                                phone = "+47 12345678",
                                email = "supplier3@test.test",
                            ),
                        identifier = UUID.randomUUID().toString(),
                        name = UUID.randomUUID().toString(),
                    ),
                )

            userRepository.createUser(
                User(
                    email = email,
                    token = password,
                    name = "User tester",
                    roles = listOf(Roles.ROLE_SUPPLIER),
                    attributes = mapOf(Pair(UserAttribute.SUPPLIER_ID, testSupplier!!.id.toString())),
                ),
            )

            userRepository.createUser(
                User(
                    email = emailAdmin,
                    token = password,
                    name = "User tester",
                    roles = listOf(Roles.ROLE_ADMIN),
                ),
            )
        }
    }

    @Test
    @Disabled
    fun apiTest() {
        runBlocking {
            val resp = loginClient.login(UsernamePasswordCredentials(email, password))
            val jwt = resp.getCookie("JWT").get().value
            val seriesRegistrationDTO =
                SeriesRegistrationDTO(
                    id = UUID.randomUUID(),
                    supplierId = testSupplier!!.id,
                    identifier = UUID.randomUUID().toString(),
                    title = "series title",
                    text = "series text",
                    isoCategory = "12345678",
                    draftStatus = DraftStatus.DONE,
                    status = SeriesStatus.ACTIVE,
                    seriesData =
                        SeriesDataDTO(
                            media =
                                setOf(
                                    MediaInfoDTO(
                                        uri = "http://example.com",
                                        type = MediaType.IMAGE,
                                        text = "image description",
                                        sourceUri = "http://example.com",
                                        source = MediaSourceType.REGISTER,
                                    ),
                                ),
                        ),
                )
            val series = apiClient.createSeries(jwt, seriesRegistrationDTO)
            series.shouldNotBeNull()
            series.status shouldBe SeriesStatus.ACTIVE
            val read = apiClient.readSeries(jwt, series.id)
            read.shouldNotBeNull()
            read.id shouldBe seriesRegistrationDTO.id
            read.seriesData.media.size shouldBe 1
            val updated =
                apiClient.updateSeries(
                    jwt,
                    read.id,
                    UpdateSeriesRegistrationDTO(title = "New title"),
                )
            updated.shouldNotBeNull()
            updated.title shouldBe "New title"

            val seriesList = apiClient.findSeries(jwt)
            seriesList.size shouldBeGreaterThan 1

            // test with products
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

            val draft1 =
                productApiClient.createDraft(
                    jwt,
                    seriesRegistrationDTO.id,
                    DraftVariantDTO(
                        supplierRef = UUID.randomUUID().toString(),
                        articleName = "variant 1",
                    ),
                )

            val readWithCount = apiClient.readSeries(jwt, updated.id)
            readWithCount.shouldNotBeNull()
            readWithCount.count shouldBeGreaterThanOrEqual 1

            val seriesWithTitle = apiClient.findSeriesWithTitle(jwt)
            seriesWithTitle.content.size shouldBe 0

            val seriesWithCapitalizedearchTerm = apiClient.findSeriesWithCapitalizedTitle(jwt)
            seriesWithCapitalizedearchTerm.content.size shouldBe 1

            val respLoginAdmin = loginClient.login(UsernamePasswordCredentials(emailAdmin, password))
            val jwtAdmin = respLoginAdmin.getCookie("JWT").get().value

            val publishedSeries = apiAdminClient.approveSeries(jwtAdmin, updated.id)
            publishedSeries.shouldNotBeNull()
            publishedSeries.draftStatus shouldBe DraftStatus.DONE
            publishedSeries.status shouldBe SeriesStatus.ACTIVE
            publishedSeries.adminStatus shouldBe AdminStatus.APPROVED

            val seriesInDraft = apiClient.setPublishedSeriesToDraft(jwt, updated.id)
            seriesInDraft.shouldNotBeNull()
            seriesInDraft.draftStatus shouldBe DraftStatus.DRAFT
            seriesInDraft.adminStatus shouldBe AdminStatus.PENDING

            val seriesWithStatus = apiClient.findSeriesByActiveOrInactiveStatus(jwt)
            seriesWithStatus.content.size shouldBeGreaterThanOrEqual 1

        }
    }
}
