package no.nav.hm.grunndata.register.event

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.hm.grunndata.rapid.dto.RapidDTO

abstract class DefaultEventHandler(private val eventItemService: EventItemService,
                                   private val objectMapper: ObjectMapper,
                                   private val registerRapidPushService: RegisterRapidPushService): EventHandler {

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(DefaultEventHandler::class.java)
    }

    override suspend fun queueDTORapidEvent(payload: EventPayload, eventName: String, extraKeyValues: Map<String, Any>) {
        LOG.info("queueDTORapidEvent for ${payload.id} with event: $eventName")
        eventItemService.createNewEventItem(
            type = getEventType(),
            oid = payload.id,
            byUser = payload.updatedByUser,
            eventName = eventName,
            payload = payload,
            extraKeyValues = extraKeyValues
        )
    }

    override fun sendRapidEvent(eventItem: EventItem): RapidDTO {
        val dto = objectMapper.readValue(eventItem.payload, getEventPayloadClass())
        val payload = dto.toRapidDTO()
        registerRapidPushService.pushToRapid(payload, eventItem)
        return payload
    }


}