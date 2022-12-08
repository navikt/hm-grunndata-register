package no.nav.hm.grunndata.register.product

import io.kotest.matchers.nulls.shouldNotBeNull
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.Supplier
import no.nav.hm.grunndata.register.supplier.SupplierRepository
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserAttribute
import no.nav.hm.grunndata.register.user.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@MicronautTest
class ProductRegistrationApiControllerTest(private val userRepository: UserRepository,
                                           private val supplierRepository: SupplierRepository,
                                           @Client("/") private val client: HttpClient) {

    val email = "user3@test.test"
    val token = "token-123"

    @BeforeEach
    fun createUserSupplier() {
        runBlocking {
            val testSupplier = supplierRepository.save(
                Supplier(
                    email = "supplier3@test.test",
                    identifier = "supplier3-unique-name",
                    name = "Supplier AS3",
                    address = "address 3",
                    homepage = "https://www.hompage.no",
                    phone = "+47 12345678"
                )
            )
            userRepository.createUser(
                User(
                    email = email, token = token, name = "User tester", roles = listOf(Roles.ROLE_SUPPLIER),
                    attributes = mapOf(Pair(UserAttribute.SUPPLIER_ID, testSupplier.id))
                )
            )
        }
    }

    @Test
    fun crudProductRegistration() {
        val request = HttpRequest.POST<Any>("/login", UsernamePasswordCredentials(email, token))
        val resp = client.toBlocking().exchange<Any, Any>(request)
        val jwt = resp.getCookie("JWT").get()
        jwt.shouldNotBeNull()
    }

}