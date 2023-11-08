package no.nav.hm.grunndata.register.event

import jakarta.inject.Singleton
import java.time.LocalDateTime
import java.util.*

@Singleton
class EventItemService(private val eventItemRepository: EventItemRepository) {

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
                                   payload: Any, extraKeyValues: Map<String, Any> = emptyMap()
    ): EventItem {
        val event = EventItem(
            oid = oid,
            type =type,
            status = EventItemStatus.PENDING,
            payload = payload,
            byUser = byUser,
            eventName = eventName,
            extraKeyValues = extraKeyValues,
            updated = LocalDateTime.now()
        )
        return eventItemRepository.save(event)
    }
}