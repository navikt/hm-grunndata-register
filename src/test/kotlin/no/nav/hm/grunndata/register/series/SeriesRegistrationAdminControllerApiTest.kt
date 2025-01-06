package no.nav.hm.grunndata.register.series

import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import jakarta.inject.Inject
import no.nav.hm.grunndata.register.product.DraftVariantDTO
import no.nav.hm.grunndata.register.product.ProductRegistrationAdminApiClient
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
class SeriesRegistrationAdminControllerApiTest {
    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesRegistrationControllerTest::class.java)
    }

    @Inject
    private lateinit var supplierApiClient: SeriesControllerApiClient

    @Inject
    private lateinit var commonApiClient: SeriesCommonControllerApiClient

    @Inject
    private lateinit var adminApiClient: SeriesAdminControllerApiClient

    @Inject
    private lateinit var productAdminApiClient: ProductRegistrationAdminApiClient

    @Inject
    private lateinit var loginClient: LoginClient

    @Inject
    private lateinit var userRepository: UserRepository

    @Inject
    private lateinit var supplierRegistrationRepository: SupplierRepository

    private val email = "series-tester@test.test123"
    private val emailAdmin = "series-admin@test.test123"
    private val password = "api-123"
    private var testSupplier: SupplierRegistration? = null

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @BeforeAll
    fun createUserSupplier() {
        runBlocking {
            LOG.info("Creating user and supplier")
            testSupplier = createTestSupplier()

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
    fun `create draft on behalf of user`() {
        runBlocking {
            val jwtAdmin =
                loginClient.login(UsernamePasswordCredentials(emailAdmin, password)).getCookie("JWT").get().value

            val series = adminApiClient.createDraft(
                jwt = jwtAdmin,
                supplierId = testSupplier!!.id,
                seriesRegistrationDTO = SeriesDraftWithDTO("titlePublished", "30090002")
            )

            val publishedSeries = commonApiClient.readSeries(jwtAdmin, series.id)
            publishedSeries.shouldNotBeNull()
            publishedSeries.status shouldBe EditStatus.EDITABLE
            publishedSeries.supplierName shouldBe testSupplier!!.name
        }
    }

    @Test
    fun `approve and publish series`() {
        runBlocking {
            val jwtAdmin =
                loginClient.login(UsernamePasswordCredentials(emailAdmin, password)).getCookie("JWT").get().value

            val series = adminApiClient.createDraft(
                jwt = jwtAdmin,
                supplierId = testSupplier!!.id,
                seriesRegistrationDTO = SeriesDraftWithDTO("titlePublished", "30090002")
            )
            adminApiClient.approveSeries(jwtAdmin, series.id)

            val publishedSeries = commonApiClient.readSeries(jwtAdmin, series.id)
            publishedSeries.shouldNotBeNull()
            publishedSeries.status shouldBe EditStatus.DONE
            publishedSeries.isPublished shouldBe true
            publishedSeries.published.shouldNotBeNull()
        }
    }

    @Test
    fun `approve and publish multiple series`() {
        runBlocking {
            val jwtSupplier =
                loginClient.login(UsernamePasswordCredentials(email, password)).getCookie("JWT").get().value

            val jwtAdmin =
                loginClient.login(UsernamePasswordCredentials(emailAdmin, password)).getCookie("JWT").get().value

            val series1 = adminApiClient.createDraft(
                jwt = jwtAdmin,
                supplierId = testSupplier!!.id,
                seriesRegistrationDTO = SeriesDraftWithDTO("titlePublishedMult1", "30090002")
            )
            val series2 = adminApiClient.createDraft(
                jwt = jwtAdmin,
                supplierId = testSupplier!!.id,
                seriesRegistrationDTO = SeriesDraftWithDTO("titlePublishedMult2", "30090002")
            )
            supplierApiClient.setSeriesToPendingApproval(jwtSupplier, series1.id)
            supplierApiClient.setSeriesToPendingApproval(jwtSupplier, series2.id)
            adminApiClient.approveMultipleSeries(jwtAdmin, listOf(series1.id, series2.id))

            val publishedSeries1 = commonApiClient.readSeries(jwtAdmin, series1.id)
            val publishedSeries2 = commonApiClient.readSeries(jwtAdmin, series2.id)

            publishedSeries1.shouldNotBeNull()
            publishedSeries1.isPublished shouldBe true

            publishedSeries2.shouldNotBeNull()
            publishedSeries2.isPublished shouldBe true
        }
    }

    @Test
    fun `reject series`() {
        runBlocking {
            val jwtSupplier =
                loginClient.login(UsernamePasswordCredentials(email, password)).getCookie("JWT").get().value

            val jwtAdmin =
                loginClient.login(UsernamePasswordCredentials(emailAdmin, password)).getCookie("JWT").get().value

            val series = supplierApiClient.createDraft(jwtSupplier, SeriesDraftWithDTO("titleRejected", "30090002"))
            supplierApiClient.setSeriesToPendingApproval(jwtSupplier, series.id)
            adminApiClient.rejectSeries(jwtAdmin, series.id, RejectSeriesDTO("rejected message"))

            val publishedSeries = commonApiClient.readSeries(jwtAdmin, series.id)
            publishedSeries.shouldNotBeNull()
            publishedSeries.status shouldBe EditStatus.REJECTED
            publishedSeries.message shouldBe "rejected message"
        }
    }

    @Test
    fun `find series pending approval`() {
        runBlocking {
            val jwtSupplier =
                loginClient.login(UsernamePasswordCredentials(email, password)).getCookie("JWT").get().value

            val jwtAdmin =
                loginClient.login(UsernamePasswordCredentials(emailAdmin, password)).getCookie("JWT").get().value

            val series = supplierApiClient.createDraft(jwtSupplier, SeriesDraftWithDTO("titlePending", "30090002"))
            supplierApiClient.createDraft(jwtSupplier, SeriesDraftWithDTO("titlePending2", "30090002"))
            supplierApiClient.setSeriesToPendingApproval(jwtSupplier, series.id)

            val pending = adminApiClient.findSeriesPendingApproval(jwtAdmin).content.filter { it.title.startsWith("titlePending") }
            pending.size shouldBe 1
            pending.first().title shouldBe "titlePending"
        }
    }

    @Test
    fun `get supplier inventory`() {
        runBlocking {
            val jwtAdmin =
                loginClient.login(UsernamePasswordCredentials(emailAdmin, password)).getCookie("JWT").get().value

            val supplier = createTestSupplier()

            adminApiClient.createDraft(
                jwt = jwtAdmin,
                supplierId = supplier.id,
                seriesRegistrationDTO = SeriesDraftWithDTO("titleInventory", "30090002")
            )
            adminApiClient.createDraft(
                jwt = jwtAdmin,
                supplierId = supplier.id,
                seriesRegistrationDTO = SeriesDraftWithDTO("titleInventory2", "30090002")
            )

            val inventory = adminApiClient.getSupplierInventory(jwtAdmin, supplier.id)
            inventory.numberOfSeries shouldBe 2
        }
    }

    @Test
    fun `move variants to series`() {
        runBlocking {
            val jwtAdmin =
                loginClient.login(UsernamePasswordCredentials(emailAdmin, password)).getCookie("JWT").get().value

            val series1 = adminApiClient.createDraft(
                jwt = jwtAdmin,
                supplierId = testSupplier!!.id,
                seriesRegistrationDTO = SeriesDraftWithDTO("titleMove1", "30090002")
            )
            val series2 = adminApiClient.createDraft(
                jwt = jwtAdmin,
                supplierId = testSupplier!!.id,
                seriesRegistrationDTO = SeriesDraftWithDTO("titleMove2", "30090002")
            )
            val variant =
                productAdminApiClient.createDraft(jwtAdmin, series1.id, DraftVariantDTO("name", "titleInventoryRef"))

            adminApiClient.moveProductVariantsToSeries(jwtAdmin, series2.id, listOf(variant.id))

            val movedToSeries = commonApiClient.readSeries(jwtAdmin, series2.id)
            movedToSeries.shouldNotBeNull()
            movedToSeries.variants.size shouldBe 1

            val movedFromSeries = commonApiClient.readSeries(jwtAdmin, series1.id)
            movedFromSeries.shouldNotBeNull()
            movedFromSeries.variants.size shouldBe 0
        }
    }

    private suspend fun createTestSupplier() = supplierRegistrationRepository.save(
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
}
