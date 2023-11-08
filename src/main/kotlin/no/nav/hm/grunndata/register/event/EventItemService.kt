package no.nav.hm.grunndata.register.event

import jakarta.inject.Singleton
import java.time.LocalDateTime

@Singleton
class EventItemService(private val eventItemRepository: EventItemRepository) {

    suspend fun getAllPendingStatus() = eventItemRepository.findByStatus(EventItemStatus.PENDING)

    suspend fun setEventItemStatusToSent(eventItem: EventItem) {
        eventItemRepository.update(eventItem.copy(status = EventItemStatus.SENT))
    }

    suspend fun deleteByStatusAndUpdatedBefore(status: EventItemStatus, updated: LocalDateTime): Int {
        return eventItemRepository.deleteByStatusAndUpdatedBefore(status, updated)
    }


}