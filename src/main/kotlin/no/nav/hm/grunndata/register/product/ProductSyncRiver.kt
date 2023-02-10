package no.nav.hm.grunndata.register.product

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Requires
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.KafkaRapid
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import no.nav.hm.grunndata.dto.ProductDTO
import no.nav.hm.rapids_rivers.micronaut.RiverHead
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Context
@Requires(bean = KafkaRapid::class)
class ProductSyncRiver(river: RiverHead,
                       private val objectMapper: ObjectMapper,
                       private val productRegistrationRepository: ProductRegistrationRepository): River.PacketListener {

    private val eventName = "hm-grunndata-db-hmdb-product-sync"

    companion object {
        private val LOG = LoggerFactory.getLogger(ProductSyncRiver::class.java)
    }
    init {
        river
            .validate { it.demandValue("eventName", eventName)}
            .validate { it.demandValue("payloadType", ProductDTO::class.java.simpleName)}
            .validate { it.demandKey("payload")}
            .validate { it.demandKey("eventId")}
            .register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val eventId = packet["eventId"].asText()
        val dto = objectMapper.treeToValue(packet["payload"], ProductDTO::class.java)
        runBlocking {
            productRegistrationRepository.findById(dto.id)?.let { inDb ->
                productRegistrationRepository.update(inDb.copy(productDTO = dto, updatedBy = dto.updatedBy,
                    updated = LocalDateTime.now(), HMSArtNr = dto.hmsArtNr, title = dto.title,
                    supplierRef = dto.supplierRef, published = dto.published, expired = dto.expired))
            } ?: productRegistrationRepository.save(
                ProductRegistration(id = dto.id, supplierId = dto.supplier.id, supplierRef = dto.supplierRef,
                    createdBy = dto.createdBy, updatedBy = dto.updatedBy, draftStatus = DraftStatus.DONE,
                    expired = dto.expired, HMSArtNr = dto.hmsArtNr, published = dto.published,
                    title = dto.title, productDTO = dto)
            )
        }
        LOG.info("product ${dto.id} with eventId $eventId synced")
    }

}
