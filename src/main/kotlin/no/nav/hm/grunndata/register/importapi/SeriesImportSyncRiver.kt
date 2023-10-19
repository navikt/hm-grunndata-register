package no.nav.hm.grunndata.register.importapi

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Requires
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.KafkaRapid
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.SeriesImportRapidDTO
import no.nav.hm.grunndata.rapid.dto.SeriesRapidDTO
import no.nav.hm.grunndata.rapid.dto.rapidDTOVersion
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.rapid.event.RapidApp
import no.nav.hm.grunndata.register.HMDB
import no.nav.hm.grunndata.register.series.SeriesRegistration
import no.nav.hm.grunndata.register.series.SeriesRegistrationDTO
import no.nav.hm.grunndata.register.series.SeriesRegistrationService
import no.nav.hm.grunndata.register.series.SeriesSyncRiver
import no.nav.hm.rapids_rivers.micronaut.RiverHead
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Context
@Requires(bean = KafkaRapid::class)
class SeriesImportSyncRiver(river: RiverHead,
                            private val objectMapper: ObjectMapper,
                            private val seriesRegistrationService: SeriesRegistrationService
): River.PacketListener {
    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesImportSyncRiver::class.java)
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

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val eventId = packet["eventId"].asText()
        val dtoVersion = packet["dtoVersion"].asLong()
        if (dtoVersion > rapidDTOVersion) LOG.warn("dto version $dtoVersion is newer than $rapidDTOVersion")
        val dto = objectMapper.treeToValue(packet["payload"], SeriesImportRapidDTO::class.java)
        runBlocking {
            val series = seriesRegistrationService.findById(dto.id)?.let { inDb ->
                seriesRegistrationService.update(
                    inDb.copy(
                        identifier = dto.seriesDTO.identifier, title = dto.seriesDTO.title, status = dto.seriesDTO.status,
                        updatedBy = dto.seriesDTO.updatedBy, updatedByUser = dto.seriesDTO.updatedBy,
                        updated = LocalDateTime.now(), expired = dto.seriesDTO.expired
                    )
                )
            } ?: seriesRegistrationService.save(
                SeriesRegistrationDTO(
                    id = dto.id,
                    supplierId = dto.seriesDTO.supplierId,
                    identifier = dto.seriesDTO.identifier,
                    title = dto.seriesDTO.title,
                    draftStatus = DraftStatus.DONE,
                    status = dto.seriesDTO.status,
                    createdBy = dto.seriesDTO.createdBy,
                    updatedBy = dto.seriesDTO.updatedBy,
                    createdByUser = dto.seriesDTO.createdBy,
                    updatedByUser = dto.seriesDTO.updatedBy,
                    createdByAdmin = false
                )
            )
           LOG.info("series import: ${series.id} with eventId $eventId synced")
        }

    }

}