package no.nav.hm.grunndata.register.product

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Requires
import no.nav.helse.rapids_rivers.*
import no.nav.hm.rapids_rivers.micronaut.RiverHead
import org.slf4j.LoggerFactory

@Context
@Requires(beans = [RapidsConnection::class])
class ProductSyncRiver(river: RiverHead, private val objectMapper: ObjectMapper): River.PacketListener {

    companion object {
        private val LOG = LoggerFactory.getLogger(ProductSyncRiver::class.java)
    }

    init {
        river
            .validate { it.demandValue("updatedBy", "HMDB") }
            .validate { it.demandKey("key") }
            .register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        LOG.info("Got packet: ${objectMapper.writeValueAsString(packet)}")
    }

}