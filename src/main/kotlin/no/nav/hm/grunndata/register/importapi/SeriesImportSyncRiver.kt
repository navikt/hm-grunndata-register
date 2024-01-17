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
import no.nav.hm.grunndata.rapid.dto.rapidDTOVersion
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.rapid.event.RapidApp
import no.nav.hm.grunndata.register.series.SeriesRegistrationDTO
import no.nav.hm.grunndata.register.series.SeriesRegistrationEventHandler
import no.nav.hm.grunndata.register.series.SeriesRegistrationService

import no.nav.hm.rapids_rivers.micronaut.RiverHead
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Context
@Requires(bean = KafkaRapid::class)
class SeriesImportSyncRiver(river: RiverHead,
                            private val objectMapper: ObjectMapper,
                            private val seriesRegistrationService: SeriesRegistrationService,
                            private val seriesRegistrationEventHandler: SeriesRegistrationEventHandler
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
                        identifier = dto.id.toString(), title = dto.title, status = dto.status,
                        updatedBy = "IMPORT", updatedByUser = "IMPORT",
                        updated = LocalDateTime.now(), expired = dto.expired
                    )
                )
            } ?: seriesRegistrationService.save(
                SeriesRegistrationDTO(
                    id = dto.id,
                    supplierId = dto.supplierId,
                    identifier = dto.id.toString(),
                    title = dto.title,
                    text = dto.text,
                    isoCategory = dto.isoCategory,
                    draftStatus = DraftStatus.DONE,
                    status = dto.status,
                    createdBy = "IMPORT",
                    updatedBy = "IMPORT",
                    createdByUser = "IMPORT",
                    updatedByUser = "IMPORT",
                    createdByAdmin = false
                )
            )
            val extraImportKeyValues =
                mapOf("transferId" to dto.transferId, "version" to dto.version)
            seriesRegistrationEventHandler.queueDTORapidEvent(series, eventName = EventName.registeredSeriesV1, extraKeyValues = extraImportKeyValues)
            LOG.info("series import: ${dto.id} transferId: ${dto.transferId} version: ${dto.version} eventId: $eventId synced")
        }
    }
}