package no.nav.hm.grunndata.register.iso.v22

import io.kotest.matchers.collections.shouldContain
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
class Iso22AdminControllerTest(
    private val client: Iso22AdminApiClient,
    private val iso22Service: Iso22Service,
    private val userRepository: UserRepository,
    private val loginClient: LoginClient,
) {

    private val email = "iso22-admin@test.test"
    private val password = "admin-123"

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @BeforeEach
    fun createAdminUser() {
        runBlocking {
            userRepository.createUser(
                User(email = email, token = password, name = "Iso22 Admin", roles = listOf(Roles.ROLE_ADMIN))
            )
        }
    }

    @Test
    fun `opprettet og endret kategori er synlig i API og cache uten restart`() {
        val jwt = loginClient.login(UsernamePasswordCredentials(email, password)).getCookie("JWT").get().value
        fun iso(title: String) = """{"isoCode":"32320101","isoTitle":"$title","createdByUser":"x","updatedByUser":"x"}"""

        client.createIso(jwt, iso("Ny kategori")).status shouldBe HttpStatus.CREATED
        client.getAllCategories().map { it.isoCode } shouldContain "32320101"
        client.getCategory("32320101").shouldNotBeNull().isoTitle shouldBe "Ny kategori"

        client.updateIso(jwt, "32320101", iso("Endret kategori")).status shouldBe HttpStatus.OK
        client.getAllCategories().first { it.isoCode == "32320101" }.isoTitle shouldBe "Endret kategori"
        runBlocking { iso22Service.lookUpCode("32320101").shouldNotBeNull().isoTitle shouldBe "Endret kategori" }
    }
}
