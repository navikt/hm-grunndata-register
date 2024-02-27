package no.nav.hm.grunndata.register.agreement

import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpStatus
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.HMDB
import no.nav.hm.grunndata.register.security.LoginClient
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserRepository
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

@MicronautTest
class DelkontraktRegistrationAdminTest(
    private val delkontraktRegistrationClient: DelkontraktRegistrationAdminClient,
    private val loginClient: LoginClient,
    private val userRepository: UserRepository
) {

    val email = "admin@test.test"
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
    fun apiDelkontraktTest() {
        val resp = loginClient.login(UsernamePasswordCredentials(email, password))
        val jwt = resp.getCookie("JWT").get().value
        val agreementId = UUID.randomUUID()
        val delkontraktId = UUID.randomUUID()
        val delkontrakt = DelkontraktRegistrationDTO(
            id = delkontraktId,
            agreementId = agreementId,
            delkontraktData = DelkontraktData(
                title = "Test title",
                description = "Test description",
                sortNr = 1,
                refNr = "1A"
            ),
            createdBy = HMDB,
            updatedBy = HMDB
        )
        runBlocking {
            var resp = delkontraktRegistrationClient.createDelkontrakt(jwt, delkontrakt)
            resp.status() shouldBe HttpStatus.CREATED
            val saved = resp.body()!!
            saved.delkontraktData.title shouldBe "Test title"
            resp = delkontraktRegistrationClient.updateDelkontrakt(
                jwt,
                saved.id,
                saved.copy(delkontraktData = saved.delkontraktData.copy(title = "Updated title"))
            )
            resp.status() shouldBe HttpStatus.OK
            val updated = resp.body()
            updated.delkontraktData.title shouldBe "Updated title"
            var find  = delkontraktRegistrationClient.getById(jwt, updated.id)
            find.status() shouldBe HttpStatus.OK
            find.body()!!.delkontraktData.title shouldBe "Updated title"
            var findAgreement = delkontraktRegistrationClient.findByAgreementId(jwt, agreementId)
            findAgreement.status() shouldBe HttpStatus.OK
            findAgreement.body()!!.size shouldBe 1
        }
    }
}