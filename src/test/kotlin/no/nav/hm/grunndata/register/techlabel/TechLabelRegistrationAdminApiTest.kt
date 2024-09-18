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

        val dto = TechLabelRegistrationDTO(
            identifier = "HMDB-20816",
            label = "HøydeLængde maks",
            guide = "Lengde",
            definition = "Lengde",
            isoCode = "09070601",
            type = "N",
            unit = "cm",
            sort = 1,
            createdByUser = "tester",
            updatedByUser = "tester",
        )
        var response = client.createTechLabel(jwt, dto)
        response.status() shouldBe HttpStatus.CREATED
        var body = response.body
        body.get().createdByUser shouldBe email
        body.get().updatedByUser shouldBe email
        response = client.updateTechLabel(jwt,dto.id, dto.copy(guide = "Høyde endret"))
        response.status() shouldBe HttpStatus.OK
        body = response.body
        body.get().guide shouldBe "Høyde endret"
        body.get().systemLabel shouldBe "hoydelaengdemaksn"

    }
}