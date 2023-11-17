//package no.nav.hm.grunndata.register.series
// Temporally disabled
//import com.fasterxml.jackson.databind.ObjectMapper
//import io.micronaut.context.annotation.Context
//import io.micronaut.context.annotation.Requires
//import kotlinx.coroutines.runBlocking
//import no.nav.helse.rapids_rivers.JsonMessage
//import no.nav.helse.rapids_rivers.KafkaRapid
//import no.nav.helse.rapids_rivers.MessageContext
//import no.nav.helse.rapids_rivers.River
//import no.nav.hm.grunndata.rapid.dto.*
//import no.nav.hm.grunndata.rapid.event.EventName
//import no.nav.hm.grunndata.rapid.event.RapidApp
//import no.nav.hm.grunndata.register.HMDB
//import no.nav.hm.rapids_rivers.micronaut.RiverHead
//import org.slf4j.LoggerFactory
//import java.time.LocalDateTime
//
//@Context
//@Requires(bean = KafkaRapid::class)
//class SeriesSyncRiver(
//    river: RiverHead,
//    private val objectMapper: ObjectMapper,
//    private val seriesRegistrationService: SeriesRegistrationService
//) : River.PacketListener {
//
//    companion object {
//        private val LOG = LoggerFactory.getLogger(SeriesSyncRiver::class.java)
//    }
//
//    init {
//        river
//            .validate { it.demandValue("createdBy", RapidApp.grunndata_db) }
//            .validate { it.demandAny("eventName", listOf(EventName.hmdbseriessyncV1)) }
//            .validate { it.demandKey("payload") }
//            .validate { it.demandKey("eventId") }
//            .validate { it.demandKey("dtoVersion") }
//            .validate { it.demandKey("createdTime") }
//            .register(this)
//    }
//
//    override fun onPacket(packet: JsonMessage, context: MessageContext) {
//        val eventId = packet["eventId"].asText()
//        val dtoVersion = packet["dtoVersion"].asLong()
//        if (dtoVersion > rapidDTOVersion) LOG.warn("dto version $dtoVersion is newer than $rapidDTOVersion")
//        val dto = objectMapper.treeToValue(packet["payload"], SeriesRapidDTO::class.java)
//        runBlocking {
//            val series = seriesRegistrationService.findById(dto.id)?.let { inDb ->
//                seriesRegistrationService.update(
//                    inDb.copy(
//                        identifier = dto.identifier, title = dto.title, text= dto.text, isoCategory = dto.isoCategory,
//                        status = dto.status, updatedBy = HMDB, updatedByUser = HMDB, updated = LocalDateTime.now()
//                    )
//                )
//            } ?: seriesRegistrationService.save(
//                SeriesRegistrationDTO(
//                    id = dto.id,
//                    supplierId = dto.supplierId,
//                    identifier = dto.identifier,
//                    title = dto.title,
//                    text = dto.text,
//                    isoCategory = dto.isoCategory,
//                    draftStatus = DraftStatus.DONE,
//                    status = dto.status,
//                    createdBy = dto.createdBy,
//                    updatedBy = dto.updatedBy,
//                    createdByUser = HMDB,
//                    updatedByUser = HMDB,
//                    createdByAdmin = false
//                )
//            )
//            LOG.info("series ${series.id} with eventId $eventId synced")
//        }
//    }
//}
//
