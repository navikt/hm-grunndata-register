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
import no.nav.hm.grunndata.rapid.event.RapidApp
import no.nav.hm.rapids_rivers.micronaut.RiverHead
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Context
@Requires(bean = KafkaRapid::class)
class ProductImportSyncRiver(river: RiverHead,
                             private val objectMapper: ObjectMapper,
                             private val productRegistrationRepository: ProductRegistrationRepository,
                             private val productRegistrationHandler: ProductRegistrationHandler): River.PacketListener {

    companion object {
        private val LOG = LoggerFactory.getLogger(ProductImportSyncRiver::class.java)
    }

    init {
        river
            .validate { it .demandValue("createdBy", RapidApp.grunndata_import)}
            .validate { it.demandAny("eventName", listOf(EventName.importedProductV1)) }
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
        val importDTO = objectMapper.treeToValue(packet["payload"], ProductImportRapidDTO::class.java)
        val registration = runBlocking {
            productRegistrationRepository.findById(importDTO.id)?.let { inDb ->
                productRegistrationRepository.update(
                    inDb.copy(
                        title = importDTO.productDTO.title,
                        articleName = importDTO.productDTO.articleName,
                        isoCategory = importDTO.productDTO.isoCategory,
                        productData = importDTO.productDTO.toProductData(),
                        seriesId = importDTO.productDTO.seriesId!!,
                        updated = importDTO.updated,
                        hmsArtNr = importDTO.productDTO.hmsArtNr,
                        supplierRef = importDTO.supplierRef,
                        supplierId = importDTO.supplierId,
                        published = importDTO.productDTO.published,
                        expired = importDTO.productDTO.expired
                    )
                )
            } ?: productRegistrationRepository.save(
                ProductRegistration(
                    id = importDTO.id,
                    title = importDTO.productDTO.title,
                    articleName = importDTO.productDTO.articleName,
                    isoCategory = importDTO.productDTO.isoCategory,
                    supplierId = importDTO.supplierId,
                    supplierRef = importDTO.supplierRef,
                    seriesId = importDTO.productDTO.seriesId!!,
                    registrationStatus = mapStatus(importDTO.productDTO.status),
                    adminStatus = AdminStatus.APPROVED,
                    adminInfo = AdminInfo(approvedBy = "AUTO", approved = LocalDateTime.now()),
                    createdBy = importDTO.productDTO.createdBy,
                    updatedBy = importDTO.productDTO.updatedBy,
                    created = importDTO.created, updated = importDTO.updated,
                    draftStatus = DraftStatus.DONE,
                    expired = importDTO.productDTO.expired,
                    hmsArtNr = importDTO.productDTO.hmsArtNr,
                    published = importDTO.productDTO.published,
                    productData = importDTO.productDTO.toProductData()
                )
            )
        }
        val extraImportKeyValues =
            mapOf("transferId" to importDTO.transferId, "version" to importDTO.version)
        productRegistrationHandler.pushToRapidIfNotDraftAndApproved(registration.toDTO(), extraImportKeyValues)
        LOG.info("imported product ${importDTO.id} with eventId $eventId and version: $${importDTO.version} synced")
    }

    private fun mapStatus(status: ProductStatus): RegistrationStatus =
        when (status) {
            ProductStatus.ACTIVE -> RegistrationStatus.ACTIVE
            ProductStatus.DELETED -> RegistrationStatus.DELETED
            else -> RegistrationStatus.INACTIVE
        }
}
