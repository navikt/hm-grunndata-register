package no.nav.hm.grunndata.register.supplier

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.SupplierDTO
import no.nav.hm.grunndata.rapid.dto.SupplierInfo
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.event.EventItem
import no.nav.hm.grunndata.register.event.EventItemService
import no.nav.hm.grunndata.register.event.EventItemType
import no.nav.hm.grunndata.register.rapid.RegisterRapidPushService
import org.slf4j.LoggerFactory


@Singleton
class SupplierRegistrationHandler(private val registerRapidPushService: RegisterRapidPushService,
                                  private val objectMapper: ObjectMapper,
                                  private val eventItemService: EventItemService
) {


    companion object {
        private val LOG = LoggerFactory.getLogger(SupplierRegistrationHandler::class.java)
    }

    fun sendRapidEvent(eventItem: EventItem) {
        val dto = objectMapper.readValue(eventItem.payload, SupplierRegistrationDTO::class.java)
        registerRapidPushService.pushToRapid(dto.toRapidDTO(), eventItem)
    }

    suspend fun queueDTORapidEvent(dto: SupplierRegistrationDTO,
                                   eventName: String = EventName.registeredSupplierV1,
                                   extraKeyValues:Map<String, Any> = emptyMap()) {
        if (dto.draftStatus == DraftStatus.DONE) {
           LOG.info("queueDTORapidEvent for ${dto.id} with draftStatus: ${dto.draftStatus} ")
            eventItemService.createNewEventItem(
                type = EventItemType.PRODUCT,
                oid = dto.id,
                byUser = dto.updatedByUser,
                eventName = eventName,
                payload = dto,
                extraKeyValues = extraKeyValues
            )
        }
    }

}

fun SupplierRegistrationDTO.toRapidDTO(): SupplierDTO = SupplierDTO (
    id = id, status = status, name=name, info = supplierData.toInfo() , identifier = identifier, created = created, updated = updated,
    createdBy = createdBy, updatedBy = updatedBy)


private fun SupplierData.toInfo(): SupplierInfo = SupplierInfo (
    address = address, postNr = postNr, postLocation = postLocation, countryCode = countryCode, email = email,
    phone = phone, homepage = homepage
)
