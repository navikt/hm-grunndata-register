package no.nav.hm.grunndata.register.product.attributes.digitalsoknad

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.rapid.dto.DigitalSoknadSortimentStatus
import no.nav.hm.grunndata.rapid.event.EventName
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDateTime
import java.util.UUID

data class DigitalSoknadSortimentDTO(
    val sortimentKategori: String,
    val postIds: List<UUID>,
)

@Singleton
open class DigitalSoknadSortimentService(
    @Value("\${digitalSoknad.url}")
    private val url : String,
    private val objectMapper: ObjectMapper,
    private val digitalSoknadRegistrationRepository: DigitalSoknadSortimentRegistrationRepository,
    private val digitalSoknadEventHandler: DigitalSoknadSortimentEventHandler,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(DigitalSoknadSortimentService::class.java)
    }

    suspend fun importAndUpdateDb() {
        val boMap = objectMapper.readTree(URI(url).toURL()).let { node ->
            require(node.isObject) { "unexpected non-object reply from digihot-sortiment" }
            val res = mutableListOf<DigitalSoknadSortimentDTO>()
            node.fields().forEachRemaining { (key, value) ->
                require(value.isArray) { "unexpected non-array reply from digihot-sortiment" }
                res.add(
                    DigitalSoknadSortimentDTO(
                        sortimentKategori = key!!,
                        postIds = value!!.mapNotNull { it.at("/postId").textValue() }.map { UUID.fromString(it) },
                    )
                )
            }
            res
        }

        val deactiveList = digitalSoknadRegistrationRepository.findByStatus(DigitalSoknadSortimentStatus.ACTIVE).filter { currentlyActive ->
            return@filter boMap.find { it.sortimentKategori == currentlyActive.sortimentKategori && it.postIds.contains(currentlyActive.postId) } == null
        }

        boMap.forEach { (sortimentKategori, postIder) ->
            postIder.forEach { postId ->
                digitalSoknadRegistrationRepository.findBySortimentKategoriAndPostId(sortimentKategori, postId)?.let { existing ->
                    if (existing.status != DigitalSoknadSortimentStatus.ACTIVE) {
                        // update digital soknad sortiment which was previously deactivated
                        LOG.info("Updating digital soknad sortiment for ${sortimentKategori}: $postId")
                        saveAndCreateEvent(existing.copy (
                            status = DigitalSoknadSortimentStatus.ACTIVE,
                            updated = LocalDateTime.now(),
                            deactivated = null,
                            updatedByUser = "system",
                        ).toDTO(), update = true)
                    }
                    existing
                } ?: run {
                    // new digital soknad sortiment
                    LOG.info("New digital soknad sortiment for ${sortimentKategori}: $postId")
                    val digitalSoknadSortimentRegistration = DigitalSoknadSortimentRegistration(sortimentKategori = sortimentKategori, postId = postId)
                    saveAndCreateEvent(digitalSoknadSortimentRegistration.toDTO(), update = false)
                }
            }
        }

        deactiveList.forEach {
            LOG.info("Deactivate digital soknad sortiment for ${it.sortimentKategori}: ${it.postId}")
            saveAndCreateEvent(it.copy(status = DigitalSoknadSortimentStatus.INACTIVE,
                updated = LocalDateTime.now(), deactivated = LocalDateTime.now()).toDTO(), update = true)
        }
    }

    @Transactional
    open suspend fun saveAndCreateEvent(digitalSoknadRegistration: DigitalSoknadSortimentRegistrationDTO, update:Boolean): DigitalSoknadSortimentRegistrationDTO {
        val saved = if (update) {
            digitalSoknadRegistrationRepository.update(digitalSoknadRegistration.toEntity())
        } else {
            digitalSoknadRegistrationRepository.save(digitalSoknadRegistration.toEntity())
        }
        val dto = saved.toDTO()
        digitalSoknadEventHandler.queueDTORapidEvent(dto, eventName = EventName.registeredDigitalSoknadSortimentV1)
        return dto
    }
}
