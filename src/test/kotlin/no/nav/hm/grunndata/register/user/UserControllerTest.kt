package no.nav.hm.grunndata.register.user

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import jakarta.inject.Inject
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.CONTEXT_PATH
import no.nav.hm.grunndata.register.security.LoginClient
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationDTO
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

@MicronautTest
class UserControllerTest(private val userRepository: UserRepository,
                         private val supplierRegistrationService: SupplierRegistrationService,
                         private val loginClient: LoginClient,
                         private val objectMapper: ObjectMapper) {

    companion object {
        private val LOG = LoggerFactory.getLogger(UserControllerTest::class.java)
    }

    @Inject
    @field:Client("$CONTEXT_PATH/")
    lateinit var client: HttpClient

    val email = "user@test.test"
    val token = "token-123"

    val email2 = "user2@test.test"
    val token2 = "token2-123"
    lateinit var uuid2: String

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @BeforeEach
    fun createUserSupplier() {
        runBlocking {
            val testSupplierRegistration = supplierRegistrationService.save(
                SupplierRegistrationDTO(
                    id = UUID.randomUUID(),
                    supplierData = SupplierData(
                        email = "supplier@test.test",
                        address = "address 1",
                        homepage = "https://www.hompage.no",
                        phone = "+47 12345678"
                    ),
                    identifier = "supplier-unique-name",
                    name = "Supplier AS"
                )
            )
            val user = User(
                email = email, token = token,
                name = "User tester", roles = listOf(Roles.ROLE_SUPPLIER),
                attributes = mapOf(Pair(UserAttribute.SUPPLIER_ID, testSupplierRegistration.id.toString()))
            )
            userRepository.createUser(user)

            LOG.info("created supplier: ${objectMapper.writeValueAsString(testSupplierRegistration)}")
            LOG.info("created user: ${objectMapper.writeValueAsString(user)}")

            val testSupplierRegistration2 = supplierRegistrationService.save(
                SupplierRegistrationDTO(
                    id = UUID.randomUUID(),
                    supplierData = SupplierData(
                        email = "supplier@test.test",
                        address = "address 1",
                        homepage = "https://www.hompage.no",
                        phone = "+47 12345678"
                    ),
                    identifier = "supplier-unique-name2",
                    name = "Supplier2 AS"
                )
            )

            val user2 = User(
                email = email2, token = token2,
                name = "User tester", roles = listOf(Roles.ROLE_SUPPLIER),
                attributes = mapOf(Pair(UserAttribute.SUPPLIER_ID, testSupplierRegistration2.id.toString()))
            )
            userRepository.createUser(user2)
            uuid2 = user2.id.toString()
            LOG.info("created supplier: ${objectMapper.writeValueAsString(testSupplierRegistration2)}")
            LOG.info("created user: ${objectMapper.writeValueAsString(user2)}")
        }
    }

    @Test
    fun userControllertest() {
        val jwt = loginClient.login(UsernamePasswordCredentials(email, token)).getCookie("JWT").get()

        val respons = client.toBlocking().exchange(
            HttpRequest.GET<List<UserDTO>>(UserController.API_V1_USER_REGISTRATIONS)
                .accept(MediaType.APPLICATION_JSON)
                .cookie(jwt), Argument.listOf(UserDTO::class.java)
        )
        respons.shouldNotBeNull()
        respons.body.shouldNotBeNull()
        respons.body.get().shouldNotBeEmpty()
        val user = respons.body.get()[0]
        user.name shouldBe "User tester"
        client.toBlocking().exchange(
            HttpRequest.PUT(UserController.API_V1_USER_REGISTRATIONS, user.copy(name = "New name"))
                .accept(MediaType.APPLICATION_JSON)
                .cookie(jwt), UserDTO::class.java
        )
            .shouldNotBeNull()
            .body().shouldNotBeNull()

        val userUri = "${UserController.API_V1_USER_REGISTRATIONS}/${user.id}"
        client.toBlocking().exchange(
            HttpRequest.GET<UserDTO>(userUri)
                .accept(MediaType.APPLICATION_JSON)
                .cookie(jwt),
            UserDTO::class.java
        )
            .shouldNotBeNull()
            .body().shouldNotBeNull()

        shouldThrow<HttpClientResponseException> {
            val otherSupplierUserUri = "${UserController.API_V1_USER_REGISTRATIONS}/${uuid2}"
            client.toBlocking().exchange(
                HttpRequest.GET<UserDTO>(otherSupplierUserUri)
                    .accept(MediaType.APPLICATION_JSON)
                    .cookie(jwt),
                UserDTO::class.java
            )
        }

        val response = client.toBlocking().exchange(
            HttpRequest.POST(UserController.API_V1_USER_REGISTRATIONS, UserRegistrationDTO(
                name = "New user",
                email = "newuser1@email.com",
                password = "aVeryStrongPassword",
                attributes = mapOf(Pair(UserAttribute.SUPPLIER_ID, user.attributes[UserAttribute.SUPPLIER_ID]!!))
            )).accept(MediaType.APPLICATION_JSON)
                .cookie(jwt), UserDTO::class.java
        )
        response.status().code shouldBe 201
        val dto = response.body()
        dto.roles shouldBe listOf(Roles.ROLE_SUPPLIER)
        dto.name shouldBe "New user"
        dto.email shouldBe "newuser1@email.com"
        dto.attributes[UserAttribute.SUPPLIER_ID] shouldBe user.attributes[UserAttribute.SUPPLIER_ID]
        val response2 = client.toBlocking().exchange(
            HttpRequest.PUT(UserController.API_V1_USER_REGISTRATIONS, dto.copy(name = "New name 2"))
                .accept(MediaType.APPLICATION_JSON)
                .cookie(jwt), UserDTO::class.java
        )
        response2.status().code shouldBe 200
        val dto2 = response2.body()
        dto2.name shouldBe "New name 2"
        dto.email shouldBe "newuser1@email.com"
        dto.attributes[UserAttribute.SUPPLIER_ID] shouldBe user.attributes[UserAttribute.SUPPLIER_ID]
    }
}
