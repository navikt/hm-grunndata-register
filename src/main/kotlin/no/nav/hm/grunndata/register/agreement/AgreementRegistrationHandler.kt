package no.nav.hm.grunndata.register.agreement

import com.fasterxml.jackson.databind.ObjectMapper
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
import org.slf4j.LoggerFactory

@Singleton
class AgreementRegistrationHandler(private val registerRapidPushService: RegisterRapidPushService,
                                   private val objectMapper: ObjectMapper,
                                   private val eventItemService: EventItemService
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(AgreementRegistrationHandler::class.java)
    }

    fun sendRapidEvent(eventItem: EventItem) {
        val dto = objectMapper.readValue(eventItem.payload, AgreementRegistrationDTO::class.java)
        registerRapidPushService.pushToRapid(dto.toRapidDTO(), eventItem)
    }

    suspend fun queueDTORapidEvent(dto: AgreementRegistrationDTO,
                                   eventName: String = EventName.registeredAgreementV1,
                                   extraKeyValues:Map<String, Any> = emptyMap()) {
        if (dto.draftStatus == DraftStatus.DONE) {
            LOG.info("queueDTORapidEvent for ${dto.id} with draft status: ${dto.draftStatus}")
            eventItemService.createNewEventItem(
                type = EventItemType.AGREEMENT,
                oid = dto.id,
                byUser = dto.updatedByUser,
                eventName = eventName,
                payload = dto,
                extraKeyValues = extraKeyValues
            )
        }
    }

    private fun AgreementRegistrationDTO.toRapidDTO(): AgreementRegistrationRapidDTO = AgreementRegistrationRapidDTO(
        id = id, draftStatus = draftStatus, created = created, updated = updated, published = published, expired = expired, createdByUser = createdByUser,
        updatedByUser = updatedByUser, createdBy = createdBy, updatedBy = updatedBy, version = version, agreementDTO = agreementData.toDTO(this)
    )

    private fun AgreementData.toDTO(registration: AgreementRegistrationDTO): AgreementDTO = AgreementDTO(
        id = registration.id, identifier = identifier, title = registration.title, resume = resume, text = text,
        status = registration.agreementStatus, reference = registration.reference, published = registration.published,
        expired = registration.expired, attachments = attachments, posts = posts, createdBy = registration.createdBy,
        updatedBy = registration.updatedBy, created = registration.created, updated = registration.updated, isoCategory = isoCategory
    )

}