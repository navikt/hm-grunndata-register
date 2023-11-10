package no.nav.hm.grunndata.register.series

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.security.authentication.Authentication
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.*
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.event.EventItem
import no.nav.hm.grunndata.register.event.EventItemService
import no.nav.hm.grunndata.register.event.EventItemType
import no.nav.hm.grunndata.register.product.ProductRegistrationDTO
import no.nav.hm.grunndata.register.product.ProductRegistrationHandler
import no.nav.hm.grunndata.register.rapid.RegisterRapidPushService
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.grunndata.register.supplier.toRapidDTO
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

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

