package no.nav.hm.grunndata.register.news

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Requires
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.KafkaRapid
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.NewsDTO
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.rapid.event.RapidApp
import no.nav.hm.rapids_rivers.micronaut.RiverHead
import org.slf4j.LoggerFactory

@Context
@Requires(bean = KafkaRapid::class)
class NewsHMDRiver(
    river: RiverHead, private val objectMapper: ObjectMapper,
    private val newsRegistrationRepository: NewsRegistrationRepository
) : River.PacketListener {

    companion object {
        private val LOG = LoggerFactory.getLogger(NewsHMDRiver::class.java)
    }
    init {
        river
            .validate { it.demandValue("createdBy", RapidApp.grunndata_db) }
            .validate { it.demandAny("eventName", listOf(EventName.hmdbnewsyncV1)) }
            .validate { it.demandKey("payload") }
            .validate { it.demandKey("eventId") }
            .register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val eventId = packet["eventId"].asText()
        val dto = objectMapper.treeToValue(packet["payload"], NewsDTO::class.java)
        LOG.info("Got news id ${dto.id} with eventId $eventId")
        runBlocking {
            newsRegistrationRepository.findById(dto.id)?.let { inDb ->
                newsRegistrationRepository.update(
                    inDb.copy(
                        title = dto.title, text = dto.text,
                        updatedByUser = dto.updatedBy,
                        status = dto.status,
                        expired = dto.expired,
                        published = dto.published,
                        updated = dto.updated
                    )
                )
            } ?: newsRegistrationRepository.save(
                NewsRegistration(
                    id = dto.id,
                    title = dto.title,
                    text = dto.text,
                    status = dto.status,
                    draftStatus = DraftStatus.DONE,
                    published = dto.published,
                    expired = dto.expired,
                    created = dto.created,
                    updated = dto.updated,
                    author = dto.author,
                    createdBy = dto.createdBy,
                    updatedBy = dto.updatedBy,
                    createdByUser = dto.createdBy,
                    updatedByUser = dto.updatedBy
                )
            )
        }
    }

}