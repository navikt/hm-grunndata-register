package no.nav.hm.grunndata.register.agreement

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.*

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
            title = "1. Delkontrakt tittel",
            description = "Description of delkontrakt 1",
            sortNr = 1
        )

        runBlocking {
            val saved = delkontraktRegistrationRepository.save(delKontrakt)
            saved.shouldNotBeNull()
            val found = delkontraktRegistrationRepository.findById(saved.id)
            found.shouldNotBeNull()
            found.title shouldBe "1. Delkontrakt tittel"
            found.description shouldBe "Description of delkontrakt 1"
            found.sortNr shouldBe 1
            found.agreementId shouldBe agreementId
        }
    }
}