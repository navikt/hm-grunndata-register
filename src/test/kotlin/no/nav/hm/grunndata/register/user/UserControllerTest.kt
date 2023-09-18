package no.nav.hm.grunndata.register.user

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.optional.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
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

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @BeforeEach
    fun createUserSupplier() {
        runBlocking {
            val testSupplierRegistration = supplierRegistrationService.save(
                SupplierRegistrationDTO(
                    id = UUID.randomUUID(),
                    supplierData = SupplierData (
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
        val changeUserResp = client.toBlocking().exchange(
            HttpRequest.PUT(UserController.API_V1_USER_REGISTRATIONS, user.copy(name = "New name"))
            .accept(MediaType.APPLICATION_JSON)
            .cookie(jwt), UserDTO::class.java)
        changeUserResp.shouldNotBeNull()
        changeUserResp.body().shouldNotBeNull()

        val userUri = "${UserController.API_V1_USER_REGISTRATIONS}/${user.id}"
        val userResponse = client.toBlocking().exchange(
            HttpRequest.GET<UserDTO>(userUri)
                .accept(MediaType.APPLICATION_JSON)
                .cookie(jwt),
            UserDTO::class.java
        )

        userResponse.shouldNotBeNull()
        userResponse.body.shouldNotBeNull()
    }
}
