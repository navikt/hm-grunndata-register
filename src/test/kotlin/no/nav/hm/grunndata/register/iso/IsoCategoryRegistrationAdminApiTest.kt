package no.nav.hm.grunndata.register.iso

import io.kotest.matchers.nulls.shouldNotBeNull
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
class IsoCategoryRegistrationAdminApiTest(private val client: IsoCategoryRegistrationAdminApiClient,
                                          private val userRepository: UserRepository,
                                          private val loginClient: LoginClient) {

    val email = "admin2@test.test"
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

        val dto = IsoCategoryRegistrationDTO(
            isoCode ="30500001",
            isoLevel = 4,
            isoTitle = "Hjelpemidler for røyking",
            isoTitleShort = "Hjelpemidler for røyking",
            isoTextShort = "Hjelpemidler for røyking",
            isoText = "Hjelpemidler som gjør det mulig for en person å røyke. Omfatter f.eks tilpassede askebegre, lightere og sigarettholdere. Smekker og forklær, se 09 03 39",
            isoTranslations = IsoTranslations(titleEn = "English title", textEn = "English text"),
            isActive = true,
            showTech = true,
            allowMulti = true,
            createdByUser = "tester",
            updatedByUser = "tester",
        )
        var response = client.createIsoCategory(jwt, dto)
        response.shouldNotBeNull()
        response.status() shouldBe HttpStatus.CREATED
        var body = response.body
        body.get().createdByUser shouldBe email
        body.get().updatedByUser shouldBe email

        val updated = dto.copy(isoTitle = "Hjelpemidler for drikking")
        response = client.updateIsoCategory(jwt, dto.isoCode, updated)
        response.shouldNotBeNull()
        response.status() shouldBe HttpStatus.OK
        response.body().isoTitle shouldBe "Hjelpemidler for drikking"

    }
}