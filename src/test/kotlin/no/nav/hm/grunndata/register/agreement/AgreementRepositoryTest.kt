package no.nav.hm.grunndata.register.agreement

import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import no.nav.hm.grunndata.register.product.REGISTER
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

@MicronautTest
class AgreementRepositoryTest(private val agreementRegistrationRepository: AgreementRegistrationRepository) {

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
            ), createdBy = REGISTER, updatedBy = REGISTER)
        val agreementRegistration = AgreementRegistration(
            id = agreementId, published = agreement.published, expired = agreement.expired, title = agreement.title,
            reference = agreement.reference, updatedByUser = "username", createdByUser = "username",
            agreementDTO = agreement
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