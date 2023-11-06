package no.nav.hm.grunndata.register.series

import io.kotest.common.runBlocking
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
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
            title = "Series 1",
            text = "Series 1 text",
            status = SeriesStatus.ACTIVE
        )
        runBlocking {
            val saved = seriesRegistrationRepository.save(series)
            val found = seriesRegistrationRepository.findById(saved.id)
            found.shouldNotBeNull()
            val identifier = seriesRegistrationRepository.findByIdentifier(series.identifier)
            identifier.shouldNotBeNull()
            found.title shouldBe identifier.title
            val updated = seriesRegistrationRepository.update(found.copy(title="Series 2"))
            updated.title shouldBe "Series 2"
            updated.status shouldBe SeriesStatus.ACTIVE
            updated.expired shouldBeAfter updated.created
        }
    }
}