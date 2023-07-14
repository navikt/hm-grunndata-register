package no.nav.hm.grunndata.register.product

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Requires
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.KafkaRapid
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import no.nav.hm.grunndata.rapid.dto.*
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.rapids_rivers.micronaut.RiverHead
import org.slf4j.LoggerFactory

@Context
@Requires(bean = KafkaRapid::class)
class ProductSyncRiver(river: RiverHead,
                       private val objectMapper: ObjectMapper,
                       private val productRegistrationRepository: ProductRegistrationRepository): River.PacketListener {

    companion object {
        private val LOG = LoggerFactory.getLogger(ProductSyncRiver::class.java)
    }

    init {
        river
            .validate { it.demandAny("eventName", listOf(EventName.hmdbproductsyncV1, EventName.expiredProductAgreementV1)) }
            .validate { it.demandKey("payload") }
            .validate { it.demandKey("eventId") }
            .validate { it.demandKey("dtoVersion") }
            .validate { it.demandKey("createdTime") }
            .register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val eventId = packet["eventId"].asText()
        val dtoVersion = packet["dtoVersion"].asLong()
        if (dtoVersion > rapidDTOVersion) LOG.warn("dto version $dtoVersion is newer than $rapidDTOVersion")
        val dto = objectMapper.treeToValue(packet["payload"], ProductRapidDTO::class.java)
        runBlocking {
            productRegistrationRepository.findById(dto.id)?.let { inDb ->
                productRegistrationRepository.update(
                    inDb.copy(
                        productData = dto.toProductData(), updatedBy = dto.updatedBy, registrationStatus = mapStatus(dto.status),
                        adminStatus = mapAdminStatus(dto.status), created = dto.created, updated = dto.updated,
                        hmsArtNr = dto.hmsArtNr, title = dto.title, supplierRef = dto.supplierRef,
                        supplierId = dto.supplier.id, published = dto.published, expired = dto.expired
                    )
                )
            } ?: productRegistrationRepository.save(
                ProductRegistration(
                    id = dto.id, supplierId = dto.supplier.id, supplierRef = dto.supplierRef,
                    registrationStatus = mapStatus(dto.status), adminStatus = mapAdminStatus(dto.status),
                    createdBy = dto.createdBy, updatedBy = dto.updatedBy, created = dto.created, updated = dto.updated,
                    draftStatus = DraftStatus.DONE, expired = dto.expired, hmsArtNr = dto.hmsArtNr,
                    published = dto.published, title = dto.title, articleName = dto.articleName,
                    productData = dto.toProductData()
                )
            )
        }
        LOG.info("product ${dto.id} with eventId $eventId synced")
    }

    private fun mapAdminStatus(status: ProductStatus): AdminStatus =
        if (status == ProductStatus.ACTIVE) AdminStatus.APPROVED else AdminStatus.PENDING

    private fun mapStatus(status: ProductStatus): RegistrationStatus =
        when (status) {
            ProductStatus.ACTIVE -> RegistrationStatus.ACTIVE
            ProductStatus.DELETED -> RegistrationStatus.DELETED
            else -> RegistrationStatus.INACTIVE
        }
}
