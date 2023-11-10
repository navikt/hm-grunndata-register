package no.nav.hm.grunndata.register.series

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.*
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.event.EventItem
import no.nav.hm.grunndata.register.event.EventItemService
import no.nav.hm.grunndata.register.event.EventItemType
import no.nav.hm.grunndata.register.event.RegisterRapidPushService
import org.slf4j.LoggerFactory

@Singleton
class SeriesRegistrationHandler(private val registerRapidPushService: RegisterRapidPushService,
                                private val eventItemService: EventItemService,
                                private val objectMapper: ObjectMapper) {
    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesRegistrationHandler::class.java)
    }

    fun sendRapidEvent(eventItem: EventItem) {
        val dto = objectMapper.readValue(eventItem.payload, SeriesRegistrationDTO::class.java)
        registerRapidPushService.pushToRapid(dto.toRapidDTO(),eventItem)
    }

    suspend fun queueDTORapidEvent(dto: SeriesRegistrationDTO,
                                   eventName: String = EventName.registeredSeriesV1,
                                   extraKeyValues:Map<String, Any> = emptyMap()) {
        if (dto.draftStatus == DraftStatus.DONE) {
            LOG.info("queueDTORapidEvent for ${dto.id} with draftStatus: ${dto.draftStatus}")
            eventItemService.createNewEventItem(
                type = EventItemType.SERIES,
                oid = dto.id,
                byUser = dto.updatedByUser,
                eventName = eventName,
                payload = dto,
                extraKeyValues = extraKeyValues
            )
        }
    }

    private fun SeriesRegistrationDTO.toRapidDTO() = SeriesRegistrationRapidDTO (
        id = id,
        supplierId = supplierId,
        identifier = identifier,
        title = title,
        text = text,
        isoCategory = isoCategory,
        draftStatus = draftStatus,
        status = status,
        created = created,
        updated = updated,
        expired = expired,
        createdBy = createdBy,
        updatedBy = updatedBy,
        updatedByUser = updatedByUser,
        createdByUser = createdByUser,
        createdByAdmin = createdByAdmin,
        version = version
    )

}

