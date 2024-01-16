package no.nav.hm.grunndata.register.bestillingsordning

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.hm.grunndata.rapid.dto.BestillingsordningRegistrationRapidDTO
import no.nav.hm.grunndata.register.event.*

class BestillingsordningEventHandler(
    private val registerRapidPushService: RegisterRapidPushService,
    private val objectMapper: ObjectMapper,
    private val eventItemService: EventItemService
) : EventHandler {

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(BestillingsordningEventHandler::class.java)
    }
    override fun sendRapidEvent(eventItem: EventItem) {
        val dto = objectMapper.readValue(eventItem.payload, BestillingsordningRegistrationDTO::class.java)
        registerRapidPushService.pushToRapid(dto.toRapidDTO(), eventItem)
    }

    override suspend fun queueDTORapidEvent(payload: EventPayload, eventName: String, extraKeyValues: Map<String, Any>) {
        val dto = payload as BestillingsordningRegistrationDTO
        LOG.info("queueDTORapidEvent for ${dto.id} - ${dto.hmsArtNr} with status ${dto.status}")
        eventItemService.createNewEventItem(
            type = EventItemType.BESTILLINGSORDNING,
            oid = dto.id,
            byUser = dto.updatedByUser,
            eventName = eventName,
            payload = dto,
            extraKeyValues = extraKeyValues
        )
    }

    private fun BestillingsordningRegistrationDTO.toRapidDTO(): BestillingsordningRegistrationRapidDTO =
        BestillingsordningRegistrationRapidDTO(
            id = id, hmsArtNr = hmsArtNr, navn = navn, status = status,
            updatedByUser = updatedByUser, createdByUser = createdByUser, created = created, updated = updated,
            deactivated = deactivated
        )
}