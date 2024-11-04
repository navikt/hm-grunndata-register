package no.nav.hm.grunndata.register.supplier

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.SupplierStatus
import no.nav.hm.grunndata.rapid.event.RapidApp
import no.nav.hm.grunndata.register.CONTEXT_PATH
import no.nav.hm.grunndata.register.security.LoginClient
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierAdminApiController.Companion.API_V1_ADMIN_SUPPLIER_REGISTRATIONS
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserRepository
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*


@MicronautTest
class SupplierRegistrationAdminApiControllerTest(private val supplierRegistrationService: SupplierRegistrationService,
                                                 private val userRepository: UserRepository,
                                                 private val supplierRegistrationAdminApiClient: SupplierRegistrationAdminApiClient,
                                                 private val loginClient: LoginClient) {

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    val email = "admintester@test.test"
    val token = "token-123"

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @BeforeEach
    fun createUserSupplier() {
        runBlocking {
            val testSupplierRegistration = supplierRegistrationService.save(
                SupplierRegistrationDTO(
                    id = UUID.randomUUID(),
                    supplierData = SupplierData(
                        email = "admintester@test.test",
                        address = "address 1",
                        homepage = "https://www.hompage.no",
                        phone = "+47 12345678"
                    ),
                    status = SupplierStatus.ACTIVE,
                    createdByUser = "admin",
                    updatedByUser = "admin",
                    createdBy = RapidApp.grunndata_register,
                    updatedBy = RapidApp.grunndata_register,
                    identifier = "adminsupplier-unique-name",
                    name = "Admin Company",

                )
            )
            userRepository.createUser(
                User(
                    email = email, token = token,
                    name = "User tester", roles = listOf(Roles.ROLE_ADMIN)
                )
            )
        }
    }


    @Test
    fun crudAPItest() {
        // login
        val jwt = loginClient.login(UsernamePasswordCredentials(email, token)).getCookie("JWT").get().value
        val supplier = SupplierRegistrationDTO(
            status = SupplierStatus.ACTIVE,
            name = "Leverandør AS",
            supplierData = SupplierData(
                address = "veien 1",
                homepage = "www.hjemmesiden.no",
                phone = "+47 12345678",
                email = "email@email.com",
            ),
            identifier = "leverandor-as"
        )
        val created  = supplierRegistrationAdminApiClient.createSupplier(jwt, supplier)
        created.shouldNotBeNull()
        created.id shouldBe supplier.id
        val found = supplierRegistrationAdminApiClient.findSuppliers(jwt, name = "Leverandør AS", page = 0, size = 10)
        found.totalSize shouldBe 1
        val updated = supplierRegistrationAdminApiClient.updateSupplier(jwt, created.id, created.copy(status = SupplierStatus.INACTIVE))
        updated.status shouldBe SupplierStatus.INACTIVE
    }
}
