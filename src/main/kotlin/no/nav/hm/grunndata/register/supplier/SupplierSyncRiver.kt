package no.nav.hm.grunndata.register.supplier

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Requires
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.KafkaRapid
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import no.nav.hm.grunndata.rapid.dto.SupplierDTO
import no.nav.hm.grunndata.rapid.dto.rapidDTOVersion
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.rapids_rivers.micronaut.RiverHead
import org.slf4j.LoggerFactory

@Context
@Requires(bean = KafkaRapid::class)
class SupplierSyncRiver(river: RiverHead,
                        private val objectMapper: ObjectMapper,
                        private val supplierRepository: SupplierRepository): River.PacketListener {

    companion object {
        private val LOG = LoggerFactory.getLogger(SupplierSyncRiver::class.java)
    }
    init {
        river
            .validate { it.demandValue("eventName", EventName.hmdbsuppliersyncV1)}
            .validate { it.demandKey("payload")}
            .validate { it.demandKey("eventId")}
            .validate { it.demandKey( "dtoVersion")}
            .register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val eventId = packet["eventId"].asText()
        val version = packet["dtoVersion"].asLong()
        val dto = objectMapper.treeToValue(packet["payload"], SupplierDTO::class.java)
        if (version > rapidDTOVersion) LOG.warn("Old dto version detected, please update to $version")
        val supplier = dto.toEntity()
        runBlocking {
            supplierRepository.findById(supplier.id)?.let { inDb ->
                supplierRepository.update(supplier.copy(created = inDb.created)) } ?: supplierRepository.save(supplier)
            LOG.info("supplier ${supplier.id} with eventId $eventId synced from HMDB")
        }
    }

}
