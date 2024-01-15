package no.nav.hm.grunndata.register.bestillingsordning

import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

@MicronautTest
class BestillingsordningRegistrationRepositoryTest(private val bestillingsordningRegistrationRepository: BestillingsordningRegistrationRepository) {


    @Test
    fun crudTest() {
        val bestilling = BestillingsordningRegistration(
            id = UUID.randomUUID(),
            hmsArtNr = "654321",
            navn = "Test bestillingsordning"
        )
        runBlocking {
            val saved = bestillingsordningRegistrationRepository.save(bestilling)
            val found = bestillingsordningRegistrationRepository.findById(saved.id)
            found.shouldNotBeNull()
            found.hmsArtNr shouldBe "654321"
            found.navn shouldBe "Test bestillingsordning"
            val updated = bestillingsordningRegistrationRepository.update(found.copy(navn = "Test bestillingsordning 2", updated = LocalDateTime.now()))
            updated.shouldNotBeNull()
            updated.updated shouldBeAfter saved.updated
            updated.updatedByUser shouldBe "system"
        }
    }

}