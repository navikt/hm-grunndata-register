package no.nav.hm.grunndata.register.supplier

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserRepository
import org.junit.jupiter.api.Test
import java.util.*
import javax.ws.rs.core.MediaType


@MicronautTest
class SupplierApiControllerTest {

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @MockBean(UserRepository::class)
    fun mockedUserRepository(): UserRepository = mockk()

    @Inject
    lateinit var userRepository: UserRepository

    @Test
    fun crudAPItest() {
        val email = "test@test.test"
        val token = "token-123"
        val uuid = UUID.randomUUID()
        client
            every {
                runBlocking {
                    userRepository.loginUser(email, token)
                }
            } answers {
                User(email = email, token = token, supplierUuid = uuid,
                    name = "test tester", roles = listOf(Roles.ROLE_ADMIN))
            }

        // login
        val creds = UsernamePasswordCredentials(email, token)
        val request  = HttpRequest.POST<Any>("/login", creds)
        val resp = client.toBlocking().exchange<Any,Any>(request)
        val jwt = resp.getCookie("JWT").get()
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