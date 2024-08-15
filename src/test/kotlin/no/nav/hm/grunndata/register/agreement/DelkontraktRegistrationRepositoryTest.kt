package no.nav.hm.grunndata.register.agreement

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

@MicronautTest
class DelkontraktRegistrationRepositoryTest(private val delkontraktRegistrationRepository: DelkontraktRegistrationRepository) {

    @Test
    fun crudTest() {
        val agreementId = UUID.randomUUID()
        val id = UUID.randomUUID()
        val delKontrakt = DelkontraktRegistration(
             id= UUID.randomUUID(),
            agreementId = agreementId,
            identifier = "qwerty",
            createdBy = "HMDB",
            updatedBy = "HMDB",
            delkontraktData =  DelkontraktData(title = "1. Delkontrakt tittel",
                description = "Description of delkontrakt 1", sortNr = 1, refNr = "1A"),
        )

        runBlocking {
            val saved = delkontraktRegistrationRepository.save(delKontrakt)
            saved.shouldNotBeNull()
            val found = delkontraktRegistrationRepository.findById(saved.id)
            found.shouldNotBeNull()
            found.identifier shouldBe "qwerty"
            found.delkontraktData.title shouldBe "1. Delkontrakt tittel"
            found.delkontraktData.description shouldBe "Description of delkontrakt 1"
            found.delkontraktData.sortNr shouldBe 1
            found.delkontraktData.refNr shouldBe "1A"
            found.agreementId shouldBe agreementId
            found.type shouldBe DelkontraktType.WITH_DELKONTRAKT
        }
    }

    @Test
    fun testExtractDelkontraktNrfromtitle() {
        val title = "1. Delkontrakt tittel"
        val title2 = "1A: Delkontrakt tittel 2"
        val title3 = "1B. Delkontrakt tittel 3"
        val title4 = "Delkontrakt 1 tittel feil"

        extractDelkontraktNrFromTitle(title) shouldBe "1"
        extractDelkontraktNrFromTitle(title2) shouldBe "1A"
        extractDelkontraktNrFromTitle(title3) shouldBe "1B"
        extractDelkontraktNrFromTitle(title4) shouldBe null

    }
}