package no.nav.hm.grunndata.register.series

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import jakarta.inject.Inject
import no.nav.hm.grunndata.register.product.DraftVariantDTO
import no.nav.hm.grunndata.register.product.ProductRegistrationAdminApiClient
import no.nav.hm.grunndata.register.product.ProductRegistrationApiClient
import no.nav.hm.grunndata.register.product.UpdateProductRegistrationDTO
import no.nav.hm.grunndata.register.security.LoginClient
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistration
import no.nav.hm.grunndata.register.supplier.SupplierRepository
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserAttribute
import no.nav.hm.grunndata.register.user.UserRepository
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import java.util.UUID

@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SeriesRegistrationCommonControllerApiTest {
    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesRegistrationCommonControllerApiTest::class.java)
    }

    @Inject
    private lateinit var commonApiClient: SeriesCommonControllerApiClient

    @Inject
    private lateinit var adminApiClient: SeriesAdminControllerApiClient

    @Inject
    private lateinit var productApiClient: ProductRegistrationApiClient

    @Inject
    private lateinit var productAdminApiClient: ProductRegistrationAdminApiClient

    @Inject
    private lateinit var loginClient: LoginClient

    @Inject
    private lateinit var userRepository: UserRepository

    @Inject
    private lateinit var supplierRegistrationRepository: SupplierRepository

    private val email = "series-tester@test.test11"
    private val email2 = "series-tester2@test.test11"
    private val emailAdmin = "series-admin@test.test11"
    private val password = "api-123"
    private var testSupplier: SupplierRegistration? = null
    private var testSupplier2: SupplierRegistration? = null

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @BeforeAll
    fun createUserSupplier() {
        runBlocking {
            LOG.info("Creating user and supplier")
            testSupplier = supplierRegistrationRepository.save(
                SupplierRegistration(
                    id = UUID.randomUUID(),
                    supplierData = SupplierData(
                        address = "address 3",
                        homepage = "https://www.hompage.no",
                        phone = "+47 12345678",
                        email = "supplier3@test.test",
                    ),
                    identifier = UUID.randomUUID().toString(),
                    name = UUID.randomUUID().toString(),
                ),
            )

            testSupplier2 = supplierRegistrationRepository.save(
                SupplierRegistration(
                    id = UUID.randomUUID(),
                    supplierData = SupplierData(
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
                    email = email2,
                    token = password,
                    name = "User tester",
                    roles = listOf(Roles.ROLE_SUPPLIER),
                    attributes = mapOf(Pair(UserAttribute.SUPPLIER_ID, testSupplier2!!.id.toString())),
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
    fun `find series by variant identifier`() {
        runBlocking {
            val jwtSupplier =
                loginClient.login(UsernamePasswordCredentials(email, password)).getCookie("JWT").get().value

            val jwtAdmin =
                loginClient.login(UsernamePasswordCredentials(emailAdmin, password)).getCookie("JWT").get().value

            val series = commonApiClient.createDraft(jwtSupplier, testSupplier!!.id, SeriesDraftWithDTO("titleFindByVar", "30090002"))
            val variant =
                productApiClient.createDraft(jwtSupplier, series.id, DraftVariantDTO("variantFindByVar", "supplierId1"))

            productAdminApiClient.updateProduct(
                jwtAdmin, variant.id, UpdateProductRegistrationDTO(
                    hmsArtNr = "hmsArtNr1",
                    articleName = variant.articleName,
                    supplierRef = variant.supplierRef,
                    productData = variant.productData
                )
            )

            val supplierRefSearch = commonApiClient.findSeriesByVariantIdentifier(jwtSupplier, "supplierId1")
            supplierRefSearch.shouldNotBeNull()

            val hmsArtNrSearch = commonApiClient.findSeriesByVariantIdentifier(jwtSupplier, "hmsArtNr1")
            hmsArtNrSearch.shouldNotBeNull()
        }
    }

    @Test
    fun `find series by field `() {
        runBlocking {
            val jwt = loginClient.login(UsernamePasswordCredentials(email, password)).getCookie("JWT").get().value

            commonApiClient.createDraft(jwt, testSupplier!!.id, SeriesDraftWithDTO("titleSearch", "30090002"))
            commonApiClient.createDraft(jwt, testSupplier!!.id, SeriesDraftWithDTO("titleSearch2", "30090002"))

            val pagedSeriesSingle = commonApiClient.findSeriesByTitle(jwt, "titleSearch2")
            pagedSeriesSingle.totalSize shouldBe 1
            pagedSeriesSingle.content.first().title shouldBe "titleSearch2"

            val pagedSeriesMultiple = commonApiClient.findSeriesByTitle(jwt, "titleSearch")
            pagedSeriesMultiple.totalSize shouldBe 2
        }
    }

    @Test
    fun `suppliers cannot read series belonging to other suppliers`() {
        runBlocking {
            val jwtSupplier =
                loginClient.login(UsernamePasswordCredentials(email, password)).getCookie("JWT").get().value

            val jwtOtherSupplier =
                loginClient.login(UsernamePasswordCredentials(email2, password)).getCookie("JWT").get().value

            val series = commonApiClient.createDraft(jwtOtherSupplier, testSupplier2!!.id, SeriesDraftWithDTO("titleOtherSupplier", "30090002"))

            val seriesOtherSupplier = commonApiClient.readSeries(jwtSupplier, series.id)
            seriesOtherSupplier.shouldBeNull()
        }
    }

    @Test
    fun `supplier cant create series for other suppliers`() {
        runBlocking {
            val jwtSupplier =
                loginClient.login(UsernamePasswordCredentials(email, password)).getCookie("JWT").get().value

            shouldThrow<HttpClientResponseException> {
                commonApiClient.createDraft(jwtSupplier, testSupplier2!!.id, SeriesDraftWithDTO("cantCreateTitle", "30090002"))
            }
        }
    }

    @Test
    fun `update series`() {
        runBlocking {
            val jwt = loginClient.login(UsernamePasswordCredentials(email, password)).getCookie("JWT").get().value

            val series = commonApiClient.createDraft(jwt, testSupplier!!.id, SeriesDraftWithDTO("titleUpdateSeries", "30090002"))

            commonApiClient.updateSeries(jwt, series.id, UpdateSeriesRegistrationDTO(text = "en tekst"))

            val updatedSeries = commonApiClient.readSeries(jwt, series.id)

            updatedSeries.shouldNotBeNull()
            updatedSeries.text shouldBe "en tekst"
        }
    }

    @Test
    fun `change expired status`() {
        runBlocking {
            val jwtSupplier =
                loginClient.login(UsernamePasswordCredentials(email, password)).getCookie("JWT").get().value

            val jwtAdmin =
                loginClient.login(UsernamePasswordCredentials(emailAdmin, password)).getCookie("JWT").get().value

            val series = commonApiClient.createDraft(jwtSupplier, testSupplier!!.id, SeriesDraftWithDTO("titleActiveStatus", "30090002"))
            adminApiClient.approveSeries(jwtAdmin, series.id)
            commonApiClient.setSeriesToDraft(jwtSupplier, series.id)

            commonApiClient.setSeriesToInactive(jwtSupplier, series.id)
            val inactivatedSeries = commonApiClient.readSeries(jwtSupplier, series.id)
            inactivatedSeries.shouldNotBeNull()
            inactivatedSeries.isExpired shouldBe true

            commonApiClient.setSeriesToActive(jwtSupplier, series.id)
            val activatedSeries = commonApiClient.readSeries(jwtSupplier, series.id)
            activatedSeries.shouldNotBeNull()
            activatedSeries.isExpired shouldBe false
        }
    }

    @Test
    fun `set deleted status`() {
        runBlocking {
            val jwt = loginClient.login(UsernamePasswordCredentials(email, password)).getCookie("JWT").get().value

            val series = commonApiClient.createDraft(jwt, testSupplier!!.id, SeriesDraftWithDTO("titleDeletedStatus", "30090002"))

            commonApiClient.setSeriesToDeleted(jwt, series.id)
            val deletedSeries = commonApiClient.readSeries(jwt, series.id)
            deletedSeries.shouldBeNull()
        }
    }

    @Test
    fun `set draft status`() {
        runBlocking {
            val jwtSupplier =
                loginClient.login(UsernamePasswordCredentials(email, password)).getCookie("JWT").get().value

            val jwtAdmin =
                loginClient.login(UsernamePasswordCredentials(emailAdmin, password)).getCookie("JWT").get().value

            val series = commonApiClient.createDraft(jwtSupplier, testSupplier!!.id, SeriesDraftWithDTO("titleDraftStatus", "30090002"))
            adminApiClient.approveSeries(jwtAdmin, series.id)
            commonApiClient.setSeriesToDraft(jwtSupplier, series.id)

            val seriesSetToDraft = commonApiClient.readSeries(jwtSupplier, series.id)
            seriesSetToDraft.shouldNotBeNull()
            seriesSetToDraft.status shouldBe EditStatus.EDITABLE
        }
    }
}
