package no.nav.hm.grunndata.register.techlabel

import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpStatus
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.security.LoginClient
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserRepository
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


@MicronautTest
class TechLabelRegistrationAdminApiTest(private val client: TechLabelRegistrationAdminApiClient,
                                        private val userRepository: UserRepository, private val loginClient: LoginClient) {

    val email = "admin3@test.test"
    val password = "admin-123"

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @BeforeEach
    fun createUserSupplier() {
        runBlocking {
            userRepository.createUser(
                User(
                    email = email, token = password, name = "User tester", roles = listOf(Roles.ROLE_ADMIN)
                )
            )
        }
    }

    @Test
    fun crudTest() {
        val resp = loginClient.login(UsernamePasswordCredentials(email, password))
        val jwt = resp.getCookie("JWT").get().value

        val dto = TechLabelCreateUpdateDTO(
            label = "HøydeLængde maks",
            type = TechLabelType.N,
            unit = "cm",
            isoCode = "09070601",
        )
        var response = client.createTechLabel(jwt, dto)
        response.status() shouldBe HttpStatus.CREATED
        var body = response.body.get()
        body.createdByUser shouldBe email
        body.updatedByUser shouldBe email
        response = client.updateTechLabel(jwt,body.id, dto.copy(label = "Høyde endret"))
        response.status() shouldBe HttpStatus.OK
        body = response.body.get()
        body.label shouldBe "Høyde endret"
        body.systemLabel shouldBe "hoydeendretn"

    }
}