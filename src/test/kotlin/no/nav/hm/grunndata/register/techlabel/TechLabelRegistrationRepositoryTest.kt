package no.nav.hm.grunndata.register.techlabel

import io.kotest.common.runBlocking
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import no.nav.hm.grunndata.register.REGISTER
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

@MicronautTest
class TechLabelRegistrationRepositoryTest(private val techLabelRegistrationRepository: TechLabelRegistrationRepository) {

    @Test
    fun crudTest() {
        val techLabel = TechLabelRegistration(id = UUID.randomUUID(), identifier = "HMDB-20815", label = "Høyde", guide="Høyde", definition = "Høyde",
            isoCode = "09070601", type = "N", unit = "cm", sort = 1, options = listOf("1", "2", "3")
        )
        runBlocking {
            val saved = techLabelRegistrationRepository.save(techLabel)
            val found = techLabelRegistrationRepository.findById(saved.id)
            found.shouldNotBeNull()
            found.identifier shouldBe "HMDB-20815"
            found.label shouldBe "Høyde"
            found.guide shouldBe "Høyde"
            found.isoCode shouldBe "09070601"
            found.type shouldBe "N"
            found.unit shouldBe "cm"
            found.definition shouldBe "Høyde"
            found.sort shouldBe 1
            found.options shouldBe listOf("1", "2", "3")
            val updated = techLabelRegistrationRepository.update(found.copy(guide = "Høyde eller noe", isKeyLabel = true, updated = LocalDateTime.now()))
            updated.shouldNotBeNull()
            updated.updated shouldBeAfter saved.updated
            updated.updatedBy shouldBe REGISTER
            updated.isKeyLabel shouldBe true
        }
    }
}