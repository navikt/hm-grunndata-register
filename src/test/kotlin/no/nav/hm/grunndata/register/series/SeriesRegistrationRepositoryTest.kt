package no.nav.hm.grunndata.register.series

import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Test
import java.util.*

@MicronautTest
class SeriesRegistrationRepositoryTest(private val seriesRegistrationRepository: SeriesRegistrationRepository) {

    @Test
    fun crudTest() {
        val series = SeriesRegistration(
            id = UUID.randomUUID(),
            supplierId = UUID.randomUUID(),
            identifier = "HMDB-123",
            name = "Series 1"
        )
        runBlocking {
            val saved = seriesRegistrationRepository.save(series)
            val found = seriesRegistrationRepository.findById(saved.id)
            found.shouldNotBeNull()
            val identifier = seriesRegistrationRepository.findByIdentifier(series.identifier)
            identifier.shouldNotBeNull()
            found.name shouldBe identifier.name
            val updated = seriesRegistrationRepository.update(found.copy(name="Series 2"))
            updated.name shouldBe "Series 2"
        }
    }
}