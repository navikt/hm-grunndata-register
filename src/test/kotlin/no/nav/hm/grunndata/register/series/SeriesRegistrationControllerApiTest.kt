package no.nav.hm.grunndata.register.series

import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import jakarta.inject.Inject
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
class SeriesRegistrationControllerApiTest {
    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesRegistrationControllerApiTest::class.java)
    }

    @Inject
    private lateinit var supplierApiClient: SeriesControllerApiClient

    @Inject
    private lateinit var commonApiClient: SeriesCommonControllerApiClient

    @Inject
    private lateinit var loginClient: LoginClient

    @Inject
    private lateinit var userRepository: UserRepository

    @Inject
    private lateinit var supplierRegistrationRepository: SupplierRepository

    private val email = "series-tester@test.test22"
    private val emailAdmin = "series-admin@test.test22"
    private val password = "api-123"
    private var testSupplier: SupplierRegistration? = null

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @BeforeAll
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
    fun `request approval`() {
        runBlocking {
            val jwt =
                loginClient.login(UsernamePasswordCredentials(email, password)).getCookie("JWT").get().value

            val series = supplierApiClient.createDraft(jwt, SeriesDraftWithDTO("titleDraftStatus", "30090002"))
            supplierApiClient.setSeriesToPendingApproval(jwt, series.id)

            val seriesSetToDraft = commonApiClient.readSeries(jwt, series.id)
            seriesSetToDraft.shouldNotBeNull()
            seriesSetToDraft.status shouldBe EditStatus.PENDING_APPROVAL
        }
    }
}
