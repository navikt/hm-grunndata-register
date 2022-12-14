package no.nav.hm.grunndata.register.user

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.mpp.log
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.security.LoginClient
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.Supplier
import no.nav.hm.grunndata.register.supplier.SupplierInfo
import no.nav.hm.grunndata.register.supplier.SupplierRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import javax.ws.rs.core.MediaType

@MicronautTest
class UserControllerTest(private val userRepository: UserRepository,
                         private val supplierRepository: SupplierRepository, private val loginClient: LoginClient) {

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    val email = "user@test.test"
    val token = "token-123"

    @BeforeEach
    fun createUserSupplier() {
        runBlocking {
            val testSupplier = supplierRepository.save(
                Supplier(
                    info = SupplierInfo(
                        email = "supplier@test.test",
                        address = "address 1",
                        homepage = "https://www.hompage.no",
                        phone = "+47 12345678"
                    ),
                    identifier = "supplier-unique-name",
                    name = "Supplier AS",

                )
            )
            userRepository.createUser(
                User(
                    email = email, token = token,
                    name = "User tester", roles = listOf(Roles.ROLE_SUPPLIER),
                    attributes = mapOf(Pair(UserAttribute.SUPPLIER_ID, testSupplier.id.toString()))
                )
            )
        }
    }

    @Test
    fun userControllertest() {
        val jwt = loginClient.login(UsernamePasswordCredentials(email, token)).getCookie("JWT").get()

        val respons = client.toBlocking().exchange(
            HttpRequest.GET<UserDTO>("/api/v1/user")
                .accept(MediaType.APPLICATION_JSON)
                .cookie(jwt), UserDTO::class.java
        )
        respons.shouldNotBeNull()
        respons.body.shouldNotBeNull()
        val user = respons.body.get()
        user.name shouldBe "User tester"
        val changeUserResp = client.toBlocking().exchange(
            HttpRequest.PUT("/api/v1/user", user.copy(name = "New name"))
            .accept(MediaType.APPLICATION_JSON)
            .cookie(jwt), UserDTO::class.java)
        changeUserResp.shouldNotBeNull()
        changeUserResp.body().shouldNotBeNull()
    }
}