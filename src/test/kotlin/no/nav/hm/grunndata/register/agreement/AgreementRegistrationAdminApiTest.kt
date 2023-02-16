package no.nav.hm.grunndata.register.agreement

import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AgreementDTO
import no.nav.hm.grunndata.rapid.dto.AgreementPost
import no.nav.hm.grunndata.register.product.REGISTER
import no.nav.hm.grunndata.register.security.LoginClient
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

@MicronautTest
class AgreementRegistrationAdminApiTest(private val apiClient: AgreementRegistrationAdminApiClient,
                                        private val loginClient: LoginClient, private val userRepository: UserRepository) {

    val email = "admin@test.test"
    val password = "admin-123"

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
    fun apiTest() {
        val resp = loginClient.login(UsernamePasswordCredentials(email, password))
        val jwt = resp.getCookie("JWT").get().value
        val agreementId = UUID.randomUUID()
        val agreement = AgreementDTO(id = agreementId, published = LocalDateTime.now(),
            expired = LocalDateTime.now().plusYears(2), title = "Title of agreement",
            text = "some text", reference = "unik-ref4", identifier = "unik-ref4", resume = "resume",
            posts = listOf(
                AgreementPost(identifier = "unik-post1", title = "Post title",
                    description = "post description", nr = 1), AgreementPost(identifier = "unik-post2", title = "Post title 2",
                    description = "post description 2", nr = 2)
            ), createdBy = REGISTER, updatedBy = REGISTER,
            created = LocalDateTime.now(),
            updated = LocalDateTime.now()
        )

        val agreementRegistration = AgreementRegistrationDTO(
            id = agreementId, published = agreement.published, expired = agreement.expired, title = agreement.title,
            reference = agreement.reference, updatedByUser = email, createdByUser = email,
            agreementDTO = agreement
        )

        val created = apiClient.createAgreement(jwt, agreementRegistration).body()
        created.shouldNotBeNull()

        val read = apiClient.getAgreementById(jwt, created.id).body()
        read.shouldNotBeNull()
        read.title shouldBe created.title
        read.createdByUser shouldBe email
        read.reference shouldBe "unik-ref4"

        val updated = apiClient.updateAgreement(jwt, read.id, read.copy(title="new title")).body()
        updated.shouldNotBeNull()
        updated.title shouldBe "new title"

        val page = apiClient.findAgreements(jwt = jwt,
            size = 20, page = 0, sort = "created,asc")
        page.totalSize shouldBe 1

        val updatedVersion = apiClient.getAgreementById(jwt, updated.id).body()
        updatedVersion.version!! shouldBeGreaterThan 0
        updatedVersion.updatedByUser shouldBe email
    }

}