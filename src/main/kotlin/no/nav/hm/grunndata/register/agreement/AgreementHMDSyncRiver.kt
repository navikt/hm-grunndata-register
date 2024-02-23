package no.nav.hm.grunndata.register.agreement

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Requires
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.KafkaRapid
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import no.nav.hm.grunndata.rapid.dto.AgreementDTO
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.rapid.event.RapidApp
import no.nav.hm.rapids_rivers.micronaut.RiverHead
import org.slf4j.LoggerFactory

@Context
@Requires(bean = KafkaRapid::class)
class AgreementSyncRiver(river: RiverHead,
                         private val objectMapper: ObjectMapper,
                         private val agreementRegistrationRepository: AgreementRegistrationRepository): River.PacketListener {


    companion object {
        private val LOG = LoggerFactory.getLogger(AgreementSyncRiver::class.java)
    }
    init {
        river
            .validate { it .demandValue("createdBy", RapidApp.grunndata_db)}
            .validate { it.demandAny("eventName", listOf(EventName.hmdbagreementsyncV1, EventName.expiredAgreementV1))}
            .validate { it.demandKey("payload")}
            .validate { it.demandKey("eventId")}
            .register(this)
    }


    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val eventId = packet["eventId"].asText()
        val dto = objectMapper.treeToValue(packet["payload"], AgreementDTO::class.java)
        runBlocking {
            agreementRegistrationRepository.findById(dto.id)?.let { inDb ->
                agreementRegistrationRepository.update(inDb.copy(agreementData = dto.toData(), reference = dto.reference,
                    agreementStatus = dto.status, title = dto.title, published = dto.published, expired = dto.expired,
                    created = dto.created, updated = dto.updated)) }
                ?: agreementRegistrationRepository.save(AgreementRegistration(
                    title = dto.title, id = dto.id, createdByUser = dto.createdBy, updatedByUser = dto.updatedBy,
                    createdBy = dto.createdBy, updatedBy = dto.updatedBy, reference = dto.reference,
                    published = dto.published, expired = dto.expired, updated = dto.updated, created = dto.created,
                    draftStatus = DraftStatus.DONE, agreementStatus = dto.status, agreementData = dto.toData()
                ))
            LOG.info("Agreement ${dto.id} with eventId $eventId synced from HMDB")
        }
    }

}
