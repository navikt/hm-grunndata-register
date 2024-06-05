package no.nav.hm.grunndata.register.series

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Requires
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.KafkaRapid
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.SeriesImportRapidDTO
import no.nav.hm.grunndata.rapid.dto.rapidDTOVersion
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.rapid.event.RapidApp
import no.nav.hm.grunndata.register.IMPORT
import no.nav.hm.grunndata.register.product.toMediaInfo
import no.nav.hm.rapids_rivers.micronaut.RiverHead
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Context
@Requires(bean = KafkaRapid::class)
class SeriesImportRiver(
    river: RiverHead,
    private val objectMapper: ObjectMapper,
    private val seriesRegistrationService: SeriesRegistrationService,
) : River.PacketListener {
    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesImportRiver::class.java)
    }

    init {
        river
            .validate { it.demandValue("createdBy", RapidApp.grunndata_import) }
            .validate { it.demandAny("eventName", listOf(EventName.importedSeriesV1)) }
            .validate { it.demandKey("payload") }
            .validate { it.demandKey("eventId") }
            .validate { it.demandKey("dtoVersion") }
            .validate { it.demandKey("createdTime") }
            .register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val eventId = packet["eventId"].asText()
        val dtoVersion = packet["dtoVersion"].asLong()
        if (dtoVersion > rapidDTOVersion) LOG.warn("dto version $dtoVersion is newer than $rapidDTOVersion")
        val dto = objectMapper.treeToValue(packet["payload"], SeriesImportRapidDTO::class.java)
        runBlocking {
            val series =
                seriesRegistrationService.findById(dto.id)?.let { inDb ->
                    seriesRegistrationService.update(
                        inDb.copy(
                            adminStatus = AdminStatus.PENDING,
                            draftStatus = DraftStatus.DONE,
                            title = dto.title,
                            text = dto.text,
                            isoCategory = dto.isoCategory,
                            seriesData =
                                SeriesDataDTO(
                                    media = dto.seriesData.media.map { it.toMediaInfo() }.toSet(),
                                    attributes =
                                        SeriesAttributesDTO(
                                            keywords = dto.seriesData.attributes.keywords?.toList(),
                                        ),
                                ),
                            status = dto.status,
                            updatedBy = IMPORT,
                            updatedByUser = IMPORT,
                            updated = LocalDateTime.now(),
                        ),
                    )
                } ?: seriesRegistrationService.save(
                    SeriesRegistrationDTO(
                        adminStatus = AdminStatus.PENDING,
                        draftStatus = DraftStatus.DONE,
                        id = dto.id,
                        supplierId = dto.supplierId,
                        identifier = dto.id.toString(),
                        title = dto.title,
                        text = dto.text,
                        isoCategory = dto.isoCategory,
                        status = dto.status,
                        seriesData =
                            SeriesDataDTO(
                                media = dto.seriesData.media.map { it.toMediaInfo() }.toSet(),
                                attributes =
                                    SeriesAttributesDTO(
                                        keywords = dto.seriesData.attributes.keywords?.toList(),
                                    ),
                            ),
                        expired = dto.expired,
                        createdBy = IMPORT,
                        updatedBy = IMPORT,
                        createdByUser = IMPORT,
                        updatedByUser = IMPORT,
                        createdByAdmin = false,
                    ),
                )
            LOG.info("series import id: ${series.id} with eventId $eventId synced")
        }
    }
}
