package no.nav.hm.grunndata.register.event

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.RapidDTO
import java.time.LocalDateTime
import java.util.*

@Singleton
class EventItemService(private val eventItemRepository: EventItemRepository,
                       private val objectMapper: ObjectMapper) {

    suspend fun getAllPendingStatus() = eventItemRepository.findByStatus(EventItemStatus.PENDING)

    suspend fun setEventItemStatusToSent(eventItem: EventItem) {
        eventItemRepository.update(eventItem.copy(status = EventItemStatus.SENT))
    }

    suspend fun deleteByStatusAndUpdatedBefore(status: EventItemStatus, updated: LocalDateTime): Int {
        return eventItemRepository.deleteByStatusAndUpdatedBefore(status, updated)
    }

    suspend fun createNewEventItem(type: EventItemType,
                                   oid: UUID,
                                   byUser: String,
                                   eventName: String,
                                   payload: RapidDTO,
                                   extraKeyValues: Map<String, Any> = emptyMap()
    ): EventItem {
        val event = EventItem(
            oid = oid,
            type =type,
            status = EventItemStatus.PENDING,
            payload = objectMapper.writeValueAsString(payload),
            byUser = byUser,
            eventName = eventName,
            extraKeyValues = extraKeyValues,
            updated = LocalDateTime.now()
        )
        return eventItemRepository.save(event)
    }
}