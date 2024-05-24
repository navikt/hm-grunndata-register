package no.nav.hm.grunndata.register.agreement

import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import no.nav.hm.grunndata.rapid.dto.AgreementDTO
import no.nav.hm.grunndata.rapid.dto.AgreementPost
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

@MicronautTest
class AgreementRepositoryTest(private val agreementRegistrationRepository: AgreementRegistrationRepository) {

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @Test
    fun crudTest() {
        val agreementId = UUID.randomUUID()
        val agreement = AgreementDTO(id = agreementId, published = LocalDateTime.now(),
            expired = LocalDateTime.now().plusYears(2), title = "Title of agreement",
            text = "some text", reference = "unik-ref1", identifier = "unik-ref1", resume = "resume",
            posts = listOf(
                AgreementPost(identifier = "unik-post1", title = "Post title",
                description = "post description", nr = 1), AgreementPost(identifier = "unik-post2", title = "Post title 2",
                    description = "post description 2", nr = 2)
            ), createdBy = REGISTER, updatedBy = REGISTER,
            created = LocalDateTime.now(), updated = LocalDateTime.now())
        val data = AgreementData(
            text = "some text", resume = "resume",
            identifier = UUID.randomUUID().toString(),
            posts = listOf(
                AgreementPost(identifier = "unik-post1", title = "Post title",
                    description = "post description", nr = 1), AgreementPost(identifier = "unik-post2", title = "Post title 2",
                    description = "post description 2", nr = 2)
            ))
        val agreementRegistration = AgreementRegistration(
            id = agreementId, published = agreement.published, expired = agreement.expired,
            title = agreement.title, reference = agreement.reference, updatedByUser = "username", createdByUser = "username", agreementData = data
        )
        runBlocking {
            val saved = agreementRegistrationRepository.save(agreementRegistration)
            saved.shouldNotBeNull()
            val read = agreementRegistrationRepository.findById(saved.id)
            read.shouldNotBeNull()
            agreementRegistrationRepository.update(read.copy(title = "ny title"))
            read.title shouldBe "Title of agreement"
            val updated = agreementRegistrationRepository.findById(read.id)
            updated.shouldNotBeNull()
            updated.title shouldBe "ny title"
        }
    }
}