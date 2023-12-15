package no.nav.hm.grunndata.register.productagreement

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.ProductAgreementRegistrationRapidDTO
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.event.EventItem
import no.nav.hm.grunndata.register.event.EventItemService
import no.nav.hm.grunndata.register.event.EventItemType
import no.nav.hm.grunndata.register.event.RegisterRapidPushService
import org.slf4j.LoggerFactory

@Singleton
class ProductAgreementRegistrationHandler(private val registerRapidPushService: RegisterRapidPushService,
                                          private val objectMapper: ObjectMapper,
                                          private val eventItemService: EventItemService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(ProductAgreementRegistrationHandler::class.java)
    }

    fun sendRapidEvent(eventItem: EventItem) {
        val dto = objectMapper.readValue(eventItem.payload, ProductAgreementRegistrationDTO::class.java)
        registerRapidPushService.pushToRapid(dto.toRapidDTO(), eventItem)
    }

    suspend fun queueDTORapidEvent(dto: ProductAgreementRegistrationDTO,
                                   eventName: String = EventName.registeredProductAgreementV1,
                                   extraKeyValues: Map<String, Any> = emptyMap()) {
        LOG.info("queueDTORapidEvent for ${dto.id} wit status ${dto.status}")
        eventItemService.createNewEventItem(
            type = EventItemType.PRODUCTAGREEMENT,
            oid = dto.id,
            byUser = "REGISTER",
            eventName = eventName,
            payload = dto,
            extraKeyValues = extraKeyValues
        )
    }


    private fun ProductAgreementRegistrationDTO.toRapidDTO(): ProductAgreementRegistrationRapidDTO =
        ProductAgreementRegistrationRapidDTO(id = id, productId = productId, agreementId = agreementId, post = post,
            rank = rank, hmsArtNr = hmsArtNr, reference = reference, status = status, title = title,
            supplierId = supplierId, supplierRef = supplierRef, created = created, updated = updated,
            published = published, expired = expired, createdBy = createdBy
    )

}