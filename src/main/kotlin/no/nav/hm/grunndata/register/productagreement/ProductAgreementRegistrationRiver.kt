package no.nav.hm.grunndata.register.productagreement

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Requires
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.*
import no.nav.hm.grunndata.rapid.dto.ProductAgreementRegistrationRapidDTO
import no.nav.hm.grunndata.rapid.dto.rapidDTOVersion
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.product.ProductRegistrationHandler
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.rapids_rivers.micronaut.RiverHead
import org.slf4j.LoggerFactory

@Context
@Requires(bean = KafkaRapid::class)
class ProductAgreementRegistrationRiver(river: RiverHead,
                                        private val objectMapper: ObjectMapper,
                                        private val productRegistrationService: ProductRegistrationService,
                                        private val productRegistrationHandler: ProductRegistrationHandler
): River.PacketListener {

    companion object {
        private val LOG = LoggerFactory.getLogger(ProductAgreementRegistrationRiver::class.java)
    }

    init {
        LOG.info("Using rapid dto version: $rapidDTOVersion")
        river
            .validate { it.demandValue("eventName", EventName.registeredProductAgreementV1)}
            .validate { it.demandValue("createdBy", "REGISTER") }
            .validate { it.demandKey("payload")}
            .validate { it.demandKey("eventId")}
            .validate { it.demandKey("dtoVersion")}
            .validate { it.demandKey( "createdTime")}
            .register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val eventId = packet["eventId"].asText()
        val dtoVersion = packet["dtoVersion"].asLong()
        val createdTime = packet["createdTime"].asLocalDateTime()
        if (dtoVersion > rapidDTOVersion) LOG.warn("dto version $dtoVersion is newer than $rapidDTOVersion")
        val dto = objectMapper.treeToValue(packet["payload"], ProductAgreementRegistrationRapidDTO::class.java)
        LOG.info("got product agreement registration id: ${dto.id} productId: ${dto.productId} supplierId: ${dto.supplierId} supplierRef: ${dto.supplierRef} " +
                "eventId $eventId eventTime: $createdTime")
        runBlocking {
            productRegistrationService.findById(dto.productId!!)?.let { product ->
                productRegistrationHandler.queueDTORapidEvent(product)
            }
        }
    }

}
