package no.nav.hm.grunndata.register.iso.v22

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
class IsoMapAdminControllerTest(
    private val client: IsoMapAdminApiClient,
    private val userRepository: UserRepository,
    private val loginClient: LoginClient
) {

    private val email = "isomap-admin@test.test"
    private val password = "admin-123"

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @BeforeEach
    fun createAdminUser() {
        runBlocking {
            userRepository.createUser(
                User(
                    email = email,
                    token = password,
                    name = "IsoMap Admin",
                    roles = listOf(Roles.ROLE_ADMIN)
                )
            )
        }
    }

    @Test
    fun crudTest() {
        val jwt = loginClient.login(UsernamePasswordCredentials(email, password))
            .getCookie("JWT").get().value

        val isoMap = IsoMapDTO(
            code16 = "122436",
            code22 = "122436",
            mapEnum = listOf(IsoMapEnum.SAME),
            verified = true
        )

        var response = client.createIsoMap(jwt, isoMap)
        response.status shouldBe HttpStatus.CREATED
        response.body.isPresent shouldBe true

        val created = response.body.get()
        created.code16 shouldBe "122436"
        created.code22 shouldBe "122436"
        created.mapEnum shouldBe listOf(IsoMapEnum.SAME)
        created.verified shouldBe true

        val updated = created.copy(
            mapEnum = listOf(IsoMapEnum.CHANGED_CODE_SAME_HEADER, IsoMapEnum.CHANGED_EXPLANATION),
            verified = false
        )

        response = client.updateIsoMap(jwt, updated.id, updated)
        response.status shouldBe HttpStatus.OK
        response.body.isPresent shouldBe true

        val updatedBody = response.body.get()
        updatedBody.id shouldBe created.id
        updatedBody.code16 shouldBe created.code16
        updatedBody.code22 shouldBe created.code22
        updatedBody.mapEnum shouldBe listOf(IsoMapEnum.CHANGED_CODE_SAME_HEADER, IsoMapEnum.CHANGED_EXPLANATION)
        updatedBody.verified shouldBe false

        val all = client.getAllIsoMaps(jwt)
        all.find { it.id == created.id }.shouldNotBeNull()
    }
}