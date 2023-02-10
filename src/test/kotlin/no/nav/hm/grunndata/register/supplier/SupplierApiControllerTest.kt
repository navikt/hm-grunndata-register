package no.nav.hm.grunndata.register.supplier

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.dto.SupplierDTO
import no.nav.hm.grunndata.dto.SupplierInfo
import no.nav.hm.grunndata.dto.SupplierStatus
import no.nav.hm.grunndata.register.product.REGISTER
import no.nav.hm.grunndata.register.security.LoginClient
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierApiController.Companion.API_V1_ADMIN_SUPPLIER_REGISTRATIONS
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*



@MicronautTest
class SupplierApiControllerTest(private val supplierRepository: SupplierRepository,
                                private val userRepository: UserRepository,
                                private val loginClient: LoginClient) {

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    val email = "admintester@test.test"
    val token = "token-123"

    @BeforeEach
    fun createUserSupplier() {
        runBlocking {
            val testSupplier = supplierRepository.save(
                Supplier(
                    info = SupplierInfo(
                        email = "admintester@test.test",
                        address = "address 1",
                        homepage = "https://www.hompage.no",
                        phone = "+47 12345678"
                    ),
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
        val jwt = loginClient.login(UsernamePasswordCredentials(email, token)).getCookie("JWT").get()
        val supplier = SupplierDTO(
            id = UUID.randomUUID(),
            status = SupplierStatus.ACTIVE,
            name = "Leverandør AS",
            info = SupplierInfo(
                address = "veien 1",
                homepage = "www.hjemmesiden.no",
                phone = "+47 12345678",
                email = "email@email.com",
            ),
            identifier = "leverandor-as",
            created = LocalDateTime.now(),
            updated = LocalDateTime.now(),
            createdBy = REGISTER,
            updatedBy = REGISTER
        )
        val respons = client.toBlocking().exchange(
            HttpRequest.POST(API_V1_ADMIN_SUPPLIER_REGISTRATIONS, supplier)
                .accept(MediaType.APPLICATION_JSON)
                .cookie(jwt), SupplierDTO::class.java
        )
        respons.shouldNotBeNull()
        respons.body.shouldNotBeNull()
        val sup = respons.body.get()
        sup.id shouldBe supplier.id
    }
}