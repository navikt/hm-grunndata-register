package no.nav.hm.grunndata.register.event

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.series.SeriesRegistrationDTO
import org.junit.jupiter.api.Test
import java.util.*

@MicronautTest
class EventItemRepositoryTest(private val eventItemRepository: EventItemRepository,
                              private val objectMapper: ObjectMapper) {

    @Test
    fun crudTest() {
        val seriesRegistrationDTO = SeriesRegistrationDTO(
            id = UUID.randomUUID(),
            supplierId = UUID.randomUUID(),
            identifier = UUID.randomUUID().toString(),
            title = "series title",
            text = "series text",
            isoCategory = "12345678",
            draftStatus = DraftStatus.DONE,
            status = SeriesStatus.ACTIVE
        )
        val item = EventItem(
            oid = UUID.randomUUID(),
            type = EventItemType.AGREEMENT,
            eventName = "test",
            byUser = "test",
            payload = objectMapper.writeValueAsString(seriesRegistrationDTO)
        )

        runBlocking {
            val saved = eventItemRepository.save(item)
            val found = eventItemRepository.findById(saved.eventId)
            found.shouldNotBeNull()
            found.type shouldBe EventItemType.AGREEMENT
            found.payload.shouldNotBeNull()
            found.status shouldBe EventItemStatus.PENDING
            val updated = eventItemRepository.update(found.copy(status = EventItemStatus.SENT))
            updated.status shouldBe EventItemStatus.SENT
            updated.payload.shouldNotBeNull()
            objectMapper.readValue(updated.payload, SeriesRegistrationDTO::class.java) shouldBe seriesRegistrationDTO
        }
    }
}