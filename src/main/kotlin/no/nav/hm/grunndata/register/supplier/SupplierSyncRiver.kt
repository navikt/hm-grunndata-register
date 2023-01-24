package no.nav.hm.grunndata.register.supplier

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Requires
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.KafkaRapid
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import no.nav.hm.rapids_rivers.micronaut.RiverHead
import org.slf4j.LoggerFactory

@Context
@Requires(bean = KafkaRapid::class)
class SupplierSyncRiver(river: RiverHead,
                        private val objectMapper: ObjectMapper,
                        private val supplierRepository: SupplierRepository): River.PacketListener {
    private val eventName = "hm-grunndata-db-hmdb-supplier-sync"

    companion object {
        private val LOG = LoggerFactory.getLogger(SupplierSyncRiver::class.java)
    }
    init {
        river
            .validate { it.demandValue("eventName", eventName)}
            .validate { it.demandKey("payload")}
            .register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val supplier = objectMapper.treeToValue(packet["payload"], SupplierDTO::class.java)
    }

}
