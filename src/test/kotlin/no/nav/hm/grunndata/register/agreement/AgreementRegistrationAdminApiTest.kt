package no.nav.hm.grunndata.register.agreement

import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AgreementPost
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.register.security.LoginClient
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserRepository
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

@MicronautTest
class AgreementRegistrationAdminApiTest(private val apiClient: AgreementRegistrationAdminApiClient,
                                        private val loginClient: LoginClient, private val userRepository: UserRepository) {

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
    fun apiTest() {
        val resp = loginClient.login(UsernamePasswordCredentials(email, password))
        val jwt = resp.getCookie("JWT").get().value
        val agreementId = UUID.randomUUID()


        val data = AgreementData(
            text = "some text",
            resume = "resume",
            identifier = UUID.randomUUID().toString(),
            posts = listOf(
                AgreementPost(identifier = "unik-post1", title = "Post title",
                    description = "post description", nr = 1), AgreementPost(identifier = "unik-post2", title = "Post title 2",
                    description = "post description 2", nr = 2)
            )
        )

        val agreementRegistration = AgreementRegistrationDTO (
            id = agreementId, published = LocalDateTime.now(), expired = LocalDateTime.now().plusYears(2),
            title = "Rammeavtale 1", reference = "unik-ref4", updatedByUser = email, createdByUser = email, agreementData =data
        )

        val agreementId2 = UUID.randomUUID()

        val data2 = AgreementData(
            text = "some text",
            resume = "resume",
            identifier = UUID.randomUUID().toString(),
            posts = listOf(
                AgreementPost(identifier = "unik-post1", title = "Post title",
                    description = "post description", nr = 1), AgreementPost(identifier = "unik-post2", title = "Post title 2",
                    description = "post description 2", nr = 2)
            )
        )

        val agreementRegistration2  = AgreementRegistrationDTO (
            id = agreementId2, published = LocalDateTime.now(), expired = LocalDateTime.now().plusYears(2),
            title = "Rammeavtale 2", reference = "unik-ref5", updatedByUser = email, createdByUser = email, agreementData =data2
        )

        val created1 = apiClient.createAgreement(jwt, agreementRegistration).body()
        created1.shouldNotBeNull()

        val created2 = apiClient.createAgreement(jwt, agreementRegistration2).body()
        created2.shouldNotBeNull()

        val read = apiClient.getAgreementById(jwt, created1.id).body()
        read.shouldNotBeNull()
        read.title shouldBe created1.title
        read.createdByUser shouldBe email
        read.reference shouldBe "unik-ref4"

        val find = apiClient.findAgreements(jwt = jwt, reference = "unik-ref4")
        find.totalSize shouldBe 1

        val updated = apiClient.updateAgreement(jwt, read.id, read.copy(title="new title", draftStatus = DraftStatus.DONE)).body()
        updated.shouldNotBeNull()
        updated.title shouldBe "new title"

        val draftStatusChanged = apiClient.updateAgreement(jwt, updated.id, updated.copy(draftStatus = DraftStatus.DRAFT)).body()
        draftStatusChanged.shouldNotBeNull()
        draftStatusChanged.draftStatus shouldBe DraftStatus.DONE


        val page = apiClient.findAgreements(jwt = jwt,
            size = 20, page = 0, sort = "created,asc")
        page.totalSize shouldBe 2

        val page2 = apiClient.findAgreements(jwt, size = 20, page = 0, sort = "created,asc", title = "Rammeavtale")
        page2.totalSize shouldBe 1

        val updatedVersion = apiClient.getAgreementById(jwt, updated.id).body()
        updatedVersion.version!! shouldBeGreaterThan 0
        updatedVersion.updatedByUser shouldBe email
    }

}