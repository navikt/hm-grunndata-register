package no.nav.hm.grunndata.register.event

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import java.time.LocalDateTime
import java.util.*

@Singleton
open class EventItemService(
    private val eventItemRepository: EventItemRepository,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(EventItemService::class.java)
    }

    private var eventHandlers = mutableMapOf<EventItemType, EventHandler>()


    suspend fun getAllPendingStatus() = eventItemRepository.findByStatus(EventItemStatus.PENDING)

    private suspend fun setEventItemStatusToSent(eventItem: EventItem) {
        eventItemRepository.update(eventItem.copy(status = EventItemStatus.SENT))
    }

    suspend fun deleteByStatusAndUpdatedBefore(status: EventItemStatus, updated: LocalDateTime): Int {
        return eventItemRepository.deleteByStatusAndUpdatedBefore(status, updated)
    }

    suspend fun createNewEventItem(
        type: EventItemType,
        oid: UUID,
        byUser: String,
        eventName: String,
        payload: EventPayload,
        extraKeyValues: Map<String, Any> = emptyMap()
    ): EventItem {
        val event = EventItem(
            oid = oid,
            type = type,
            status = EventItemStatus.PENDING,
            payload = objectMapper.writeValueAsString(payload),
            byUser = byUser,
            eventName = eventName,
            extraKeyValues = extraKeyValues,
            updated = LocalDateTime.now()
        )
        return eventItemRepository.save(event)
    }

    @Transactional
    open suspend fun sendRapidEvent(eventItem: EventItem) {
        if (eventHandlers[eventItem.type] != null) {
            eventHandlers[eventItem.type]?.sendRapidEvent(eventItem)
            setEventItemStatusToSent(eventItem)
        }
        else {
            LOG.error("No EventHandler found for type: ${eventItem.type}")
        }
    }

    fun addEventHandler(bean: EventHandler) {
        eventHandlers[bean.getEventType()] = bean
    }


}