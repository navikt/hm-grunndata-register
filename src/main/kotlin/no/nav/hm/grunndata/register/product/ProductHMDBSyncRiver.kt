package no.nav.hm.grunndata.register.product

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Requires
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.KafkaRapid
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.AgreementStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.rapid.dto.ProductRapidDTO
import no.nav.hm.grunndata.rapid.dto.ProductStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.rapid.dto.rapidDTOVersion
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.rapid.event.RapidApp
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationService
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationDTO
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationService
import no.nav.hm.grunndata.register.series.SeriesDataDTO
import no.nav.hm.grunndata.register.series.SeriesRegistrationDTO
import no.nav.hm.grunndata.register.series.SeriesRegistrationService
import no.nav.hm.rapids_rivers.micronaut.RiverHead
import org.slf4j.LoggerFactory

@Context
@Requires(bean = KafkaRapid::class)
class ProductHMDBSyncRiver(
    river: RiverHead,
    private val objectMapper: ObjectMapper,
    private val productRegistrationRepository: ProductRegistrationRepository,
    private val seriesRegistrationService: SeriesRegistrationService,
    private val agreementRegistrationService: AgreementRegistrationService,
    private val productAgreementRegistrationService: ProductAgreementRegistrationService
) : River.PacketListener {

    companion object {
        private val LOG = LoggerFactory.getLogger(ProductHMDBSyncRiver::class.java)
    }

    init {
        river
            .validate { it.demandValue("createdBy", RapidApp.grunndata_db) }
            .validate { it.demandAny("eventName", listOf(EventName.hmdbproductsyncV1)) }
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
        LOG.info("syncing product ${dto.id} with eventId $eventId, seriesUUID = ${dto.seriesUUID}, seriesId = ${dto.seriesId}")
        LOG.info("Detailed event info: ${packet.toJson()}")
        runBlocking {
            productRegistrationRepository.findById(dto.id)?.let { inDb ->
                productRegistrationRepository.update(
                    inDb.copy(
                        seriesId = dto.seriesId!!, seriesUUID = dto.seriesUUID?:dto.id, productData = dto.toProductData(),
                        updatedBy = dto.updatedBy, registrationStatus = mapStatus(dto.status),
                        adminStatus = mapAdminStatus(dto.status), created = dto.created, updated = dto.updated,
                        hmsArtNr = dto.hmsArtNr, title = dto.title, supplierRef = dto.supplierRef,
                        supplierId = dto.supplier.id, published = dto.published, expired = dto.expired
                    )
                )

            } ?: productRegistrationRepository.save(
                ProductRegistration(
                    id = dto.id,
                    isoCategory = dto.isoCategory,
                    supplierId = dto.supplier.id,
                    supplierRef = dto.supplierRef,
                    seriesId = dto.seriesId!!,
                    seriesUUID = dto.seriesUUID?:dto.id,
                    registrationStatus = mapStatus(dto.status),
                    adminStatus = mapAdminStatus(dto.status),
                    createdBy = dto.createdBy,
                    updatedBy = dto.updatedBy,
                    created = dto.created,
                    updated = dto.updated,
                    draftStatus = DraftStatus.DONE,
                    expired = dto.expired,
                    hmsArtNr = dto.hmsArtNr,
                    published = dto.published,
                    title = dto.title,
                    articleName = dto.articleName,
                    productData = dto.toProductData()
                )
            )
            val series = dto.seriesUUID?.let { uuid ->
                seriesRegistrationService.findById(uuid)?.let { inDb ->
                    if (inDb.isoCategory != dto.isoCategory || inDb.title != dto.title ||
                        inDb.text != dto.attributes.text
                        || inDb.expired != dto.expired) {
                        seriesRegistrationService.update(
                            inDb.copy(
                                title = dto.title,
                                text = dto.attributes.text ?: "",
                                isoCategory = dto.isoCategory,
                                updated = dto.updated,
                                expired = dto.expired,
                                updatedBy = dto.updatedBy,
                                seriesData = SeriesDataDTO(media = dto.media.map { it.toMediaInfo() }.toSet()
                            )
                        ))
                    }
                    else inDb

                } ?: seriesRegistrationService.save(
                    SeriesRegistrationDTO(
                        id = uuid,
                        supplierId = dto.supplier.id,
                        identifier = dto.seriesIdentifier ?: uuid.toString(),
                        title = dto.title,
                        text = dto.attributes.text ?: "",
                        isoCategory = dto.isoCategory,
                        draftStatus = DraftStatus.DONE,
                        status = SeriesStatus.ACTIVE,
                        adminStatus = AdminStatus.APPROVED,
                        createdBy = dto.createdBy,
                        updatedBy = dto.updatedBy,
                        created = dto.created,
                        updated = dto.updated,
                        published = dto.published,
                        expired = dto.expired,
                        seriesData = SeriesDataDTO(media = dto.media.map { it.toMediaInfo() }.toSet())
                    )
                )
            }

            dto.agreements.forEach { agreementInfo ->
                LOG.info("updating product agreement for product ${dto.id} with agreement_id ${agreementInfo.id} and postId ${agreementInfo.postId}")
                agreementRegistrationService.findById(agreementInfo.id)?.let { agreement ->
                    productAgreementRegistrationService.findBySupplierIdAndSupplierRefAndAgreementIdAndPostIdAndRank(
                        dto.supplier.id, dto.supplierRef, agreementInfo.id, agreementInfo.postId!!, agreementInfo.rank
                    )?.let { inDb ->
                        productAgreementRegistrationService.update(
                            inDb.copy(
                                supplierId = dto.supplier.id,
                                supplierRef = dto.supplierRef,
                                productId = dto.id,
                                seriesUuid = series?.id,
                                title = dto.title,
                                articleName = dto.articleName,
                                agreementId = agreement.id,
                                hmsArtNr = dto.hmsArtNr,
                                post = agreementInfo.postNr,
                                rank = agreementInfo.rank,
                                postId = agreementInfo.postId,
                                reference = agreement.reference,
                                expired = agreement.expired,
                                created = dto.created,
                                updated = dto.updated,
                                createdBy = dto.createdBy,
                                published = agreement.published,
                                status = if (agreement.agreementStatus == AgreementStatus.ACTIVE)
                                    ProductAgreementStatus.ACTIVE else ProductAgreementStatus.INACTIVE
                            )
                        )
                    } ?: productAgreementRegistrationService.save(
                        ProductAgreementRegistrationDTO(
                            supplierId = dto.supplier.id,
                            supplierRef = dto.supplierRef,
                            productId = dto.id,
                            seriesUuid = series?.id,
                            title = dto.title,
                            articleName = dto.articleName,
                            agreementId = agreement.id,
                            hmsArtNr = dto.hmsArtNr,
                            post = agreementInfo.postNr,
                            rank = agreementInfo.rank,
                            postId = agreementInfo.postId,
                            reference = agreement.reference,
                            expired = agreement.expired,
                            created = dto.created,
                            updated = dto.updated,
                            createdBy = dto.createdBy,
                            published = agreement.published,
                            status = if (agreement.agreementStatus == AgreementStatus.ACTIVE)
                                ProductAgreementStatus.ACTIVE else ProductAgreementStatus.INACTIVE
                        )
                    )
                }
            }
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
