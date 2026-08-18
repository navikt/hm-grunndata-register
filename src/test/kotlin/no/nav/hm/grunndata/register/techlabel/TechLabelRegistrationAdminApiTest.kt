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
            if (userRepository.findByEmailIgnoreCase(email) == null) {
                userRepository.createUser(
                    User(
                        email = email, token = password, name = "User tester", roles = listOf(Roles.ROLE_ADMIN)
                    )
                )
            }
        }
    }

    @Test
    fun crudTest() {
        val resp = loginClient.login(UsernamePasswordCredentials(email, password))
        val jwt = resp.getCookie("JWT").get().value

        val dto = TechLabelCreateUpdateDTO(
            label = "HøydeLængde maks",
            type = TechLabelType.N,
            definition = "En beskrivelse",
            unit = "cm",
            isoCode = "09070601",
            sort = 1,
            guide = "Her skal det stå en veiledningstekst",
            required = true
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
        body.sort shouldBe 1
        body.required shouldBe true
        body.guide shouldBe "Her skal det stå en veiledningstekst"
        body.definition shouldBe "En beskrivelse"

    }

    @Test
    fun updateSectionForLabelTest() {
        val resp = loginClient.login(UsernamePasswordCredentials(email, password))
        val jwt = resp.getCookie("JWT").get().value

        val dto1 = TechLabelCreateUpdateDTO(
            label = "Setebredde min",
            type = TechLabelType.N,
            unit = "cm",
            isoCode = "12221801",
            sort = 1,
            required = false
        )
        val dto2 = dto1.copy(isoCode = "12221802")
        val created1 = client.createTechLabel(jwt, dto1).body.get()
        val created2 = client.createTechLabel(jwt, dto2).body.get()
        created1.section shouldBe null
        created2.section shouldBe null

        val sectionResponse = client.updateSection(jwt, TechLabelSectionUpdateDTO(label = "Setebredde min", section = "Sete"))
        sectionResponse.status() shouldBe HttpStatus.OK
        val updatedList = sectionResponse.body.get()
        updatedList.size shouldBe 2
        updatedList.forEach { it.section shouldBe "Sete" }

        client.getTechLabelById(jwt, created1.id).body.get().section shouldBe "Sete"
        client.getTechLabelById(jwt, created2.id).body.get().section shouldBe "Sete"
    }
}