package no.nav.hm.grunndata.register.agreement

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Requires
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.KafkaRapid
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import no.nav.hm.grunndata.register.product.DraftStatus
import no.nav.hm.rapids_rivers.micronaut.RiverHead
import org.slf4j.LoggerFactory

@Context
@Requires(bean = KafkaRapid::class)
class AgreementSyncRiver(river: RiverHead,
                         private val objectMapper: ObjectMapper,
                         private val agreementRegistrationRepository: AgreementRegistrationRepository): River.PacketListener {

    private val eventName = "hm-grunndata-db-hmdb-agreement-sync"

    companion object {
        private val LOG = LoggerFactory.getLogger(AgreementSyncRiver::class.java)
    }
    init {
        river
            .validate { it.demandValue("eventName", eventName)}
            .validate { it.demandValue("payloadType", AgreementDTO::class.java.simpleName)}
            .validate { it.demandKey("payload")}
            .validate { it.demandKey("eventId")}
            .register(this)
    }


    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val eventId = packet["eventId"].asText()
        val dto = objectMapper.treeToValue(packet["payload"], AgreementDTO::class.java)
        runBlocking {
            agreementRegistrationRepository.findById(dto.id)?.let { inDb ->
                agreementRegistrationRepository.update(inDb.copy(agreementDTO = dto)) }
                ?: agreementRegistrationRepository.save(AgreementRegistration(
                    title = dto.title, id = dto.id, createdByUser = dto.createdBy, updatedByUser = dto.updatedBy,
                    createdBy = dto.createdBy, updatedBy = dto.updatedBy, reference = dto.reference,
                    published = dto.published, expired = dto.expired, status = AgreementStatus.ACTIVE,
                    draftStatus = DraftStatus.DONE, agreementDTO = dto
                ))
            LOG.info("Agreement ${dto.id} with eventId $eventId synced from HMDB")
        }
    }

}
