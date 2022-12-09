package no.nav.hm.grunndata.register.supplier

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.product.getLoginCookie
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import javax.ws.rs.core.MediaType


@MicronautTest
class SupplierApiControllerTest(private val supplierRepository: SupplierRepository,
                                private val userRepository: UserRepository) {

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
                    email = "admintester@test.test",
                    identifier = "adminsupplier-unique-name",
                    name = "Admin Company",
                    address = "address 1",
                    homepage = "https://www.hompage.no",
                    phone = "+47 12345678"
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
        val jwt = getLoginCookie(client, email, token)
        val supplier = SupplierDTO(
            id = UUID.randomUUID(),
            status = SupplierStatus.ACTIVE,
            name = "Leverandør AS",
            address = "veien 1",
            homepage = "www.hjemmesiden.no",
            phone = "+47 12345678",
            email = "email@email.com",
            identifier = "leverandor-as"
        )
        val respons = client.toBlocking().exchange(
            HttpRequest.POST("/api/v1/supplier", supplier)
                .accept(MediaType.APPLICATION_JSON)
                .cookie(jwt), SupplierDTO::class.java
        )
        respons.shouldNotBeNull()
        respons.body.shouldNotBeNull()
        val sup = respons.body.get()
        sup.id shouldBe supplier.id
    }
}