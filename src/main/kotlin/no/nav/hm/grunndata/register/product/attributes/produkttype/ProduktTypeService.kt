package no.nav.hm.grunndata.register.product.attributes.produkttype

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.rapid.dto.ProduktTypeStatus
import no.nav.hm.grunndata.rapid.event.EventName
import org.slf4j.LoggerFactory
import java.net.URL
import java.time.LocalDateTime

data class ProduktTypeDTO(
    val isokode: String,
    val produkttype: String,
)

@Singleton
open class ProduktTypeService(
    @Value("\${digihotSortiment.produktType}")
    private val url : String,
    private val objectMapper: ObjectMapper,
    private val produktTypeRegistrationRepository: ProduktTypeRegistrationRepository,
    private val produktTypeEventHandler: ProduktTypeEventHandler,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(ProduktTypeService::class.java)
    }

    suspend fun importAndUpdateDb() {
        val boMap = objectMapper.readValue(URL(url), object : TypeReference<List<ProduktTypeDTO>>(){}).associateBy { it.isokode }

        val deactiveList = produktTypeRegistrationRepository.findByStatus(ProduktTypeStatus.ACTIVE).filter { currentlyActive ->
            return@filter boMap.find { it.sortimentKategori == currentlyActive.sortimentKategori && it.postIds.contains(currentlyActive.postId) } == null
        }

        boMap.forEach { (sortimentKategori, postIder) ->
            postIder.forEach { postId ->
                produktTypeRegistrationRepository.findByKategoriAndPostId(sortimentKategori, postId)?.let { existing ->
                    if (existing.status != ProduktTypeStatus.ACTIVE) {
                        // update produkt type which was previously deactivated
                        LOG.info("Updating produkt type for ${sortimentKategori}: $postId")
                        saveAndCreateEvent(existing.copy (
                            status = ProduktTypeStatus.ACTIVE,
                            updated = LocalDateTime.now(),
                            deactivated = null,
                            updatedByUser = "system",
                        ).toDTO(), update = true)
                    }
                    existing
                } ?: run {
                    // new produkt type
                    LOG.info("New produkt type for ${sortimentKategori}: $postId")
                    val produktTypeRegistration = ProduktTypeRegistration(sortimentKategori = sortimentKategori, postId = postId)
                    saveAndCreateEvent(produktTypeRegistration.toDTO(), update = false)
                }
            }
        }

        deactiveList.forEach {
            LOG.info("Deactivate produkt type for ${it.sortimentKategori}: ${it.postId}")
            saveAndCreateEvent(it.copy(status = ProduktTypeStatus.INACTIVE,
                updated = LocalDateTime.now(), deactivated = LocalDateTime.now()).toDTO(), update = true)
        }
    }

    @Transactional
    open suspend fun saveAndCreateEvent(produktTypeRegistration: ProduktTypeRegistrationDTO, update:Boolean): ProduktTypeRegistrationDTO {
        val saved = if (update) {
            produktTypeRegistrationRepository.update(produktTypeRegistration.toEntity())
        } else {
            produktTypeRegistrationRepository.save(produktTypeRegistration.toEntity())
        }
        val dto = saved.toDTO()
        produktTypeEventHandler.queueDTORapidEvent(dto, eventName = EventName.registeredProduktTypeV1)
        return dto
    }
}
