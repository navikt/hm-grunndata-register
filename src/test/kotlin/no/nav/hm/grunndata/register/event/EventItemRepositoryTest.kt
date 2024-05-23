package no.nav.hm.grunndata.register.event

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.MediaSourceType
import no.nav.hm.grunndata.rapid.dto.MediaType
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.product.MediaInfoDTO
import no.nav.hm.grunndata.register.series.SeriesDataDTO
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
            status = SeriesStatus.ACTIVE,
            seriesData = SeriesDataDTO(media = setOf(
                MediaInfoDTO(uri = "http://example.com", type = MediaType.IMAGE, text = "image description", sourceUri = "http://example.com",  source = MediaSourceType.REGISTER)
            ))
        )
        val extraKeyValues = mapOf("key" to "value")
        val item = EventItem(
            oid = UUID.randomUUID(),
            type = EventItemType.AGREEMENT,
            eventName = "test",
            byUser = "test",
            extraKeyValues = extraKeyValues,
            payload = objectMapper.writeValueAsString(seriesRegistrationDTO)
        )

        runBlocking {
            val saved = eventItemRepository.save(item)
            val found = eventItemRepository.findById(saved.eventId)
            found.shouldNotBeNull()
            found.type shouldBe EventItemType.AGREEMENT
            found.payload.shouldNotBeNull()
            found.status shouldBe EventItemStatus.PENDING
            found.extraKeyValues shouldBe extraKeyValues
            found.extraKeyValues["key"] shouldBe "value"
            val updated = eventItemRepository.update(found.copy(status = EventItemStatus.SENT))
            updated.status shouldBe EventItemStatus.SENT
            updated.payload.shouldNotBeNull()
            objectMapper.readValue(updated.payload, SeriesRegistrationDTO::class.java) shouldBe seriesRegistrationDTO
        }
    }
}