package no.nav.hm.grunndata.register.product.attributes.paakrevdgodkjenningskurs

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.rapid.dto.PaakrevdGodkjenningskursStatus
import no.nav.hm.grunndata.rapid.event.EventName
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDateTime

data class PaakrevdGodkjenningskursDTO(
    val isokode: String,
    val tittel: String,
    val kursId: Int,
)

@Singleton
open class PaakrevdGodkjenningskursService(
    @Value("\${digihotSortiment.paakrevdGodkjenningskurs}")
    private val url : String,
    private val objectMapper: ObjectMapper,
    private val paakrevdGodkjenningskursRegistrationRepository: PaakrevdGodkjenningskursRegistrationRepository,
    private val paakrevdGodkjenningskursEventHandler: PaakrevdGodkjenningskursEventHandler,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(PaakrevdGodkjenningskursService::class.java)
    }

    suspend fun importAndUpdateDb() {
        val boMap = URI(url).toURL().openStream().use { inputStream ->
            objectMapper.readValue(inputStream, object : TypeReference<List<PaakrevdGodkjenningskursDTO>>() {})
        }.associateBy { it.isokode }

        val deactiveList = paakrevdGodkjenningskursRegistrationRepository.findByStatus(PaakrevdGodkjenningskursStatus.ACTIVE).filter { currentlyActive ->
            return@filter !boMap.containsKey(currentlyActive.isokode)
        }

        boMap.forEach { (isokode, paakrevdGodkjenningskursDTO) ->
            paakrevdGodkjenningskursRegistrationRepository.findByIsokode(isokode)?.let { existing ->
                if (
                    existing.status != PaakrevdGodkjenningskursStatus.ACTIVE ||
                    existing.tittel != paakrevdGodkjenningskursDTO.tittel ||
                    existing.kursId != paakrevdGodkjenningskursDTO.kursId
                ) {
                    // update paakrevd godkjenningskurs which was previously deactivated
                    LOG.info("Updating paakrevd godkjenningskurs for: $paakrevdGodkjenningskursDTO")
                    saveAndCreateEvent(existing.copy (
                        tittel = paakrevdGodkjenningskursDTO.tittel,
                        kursId = paakrevdGodkjenningskursDTO.kursId,
                        status = PaakrevdGodkjenningskursStatus.ACTIVE,
                        updated = LocalDateTime.now(),
                        deactivated = null,
                        updatedByUser = "system",
                    ).toDTO(), update = true)
                }
                existing
            } ?: run {
                // new paakrevd godkjenningskurs
                LOG.info("New paakrevd godkjenningskurs for: $paakrevdGodkjenningskursDTO")
                val paakrevdGodkjenningskursRegistration = PaakrevdGodkjenningskursRegistration(
                    isokode = paakrevdGodkjenningskursDTO.isokode,
                    tittel = paakrevdGodkjenningskursDTO.tittel,
                    kursId = paakrevdGodkjenningskursDTO.kursId,
                )
                saveAndCreateEvent(paakrevdGodkjenningskursRegistration.toDTO(), update = false)
            }
        }

        deactiveList.forEach {
            LOG.info("Deactivate paakrevd godkjenningskurs for isokode: ${it.isokode}")
            saveAndCreateEvent(it.copy(status = PaakrevdGodkjenningskursStatus.INACTIVE,
                updated = LocalDateTime.now(), deactivated = LocalDateTime.now()).toDTO(), update = true)
        }
    }

    @Transactional
    open suspend fun saveAndCreateEvent(paakrevdGodkjenningskursRegistration: PaakrevdGodkjenningskursRegistrationDTO, update:Boolean): PaakrevdGodkjenningskursRegistrationDTO {
        val saved = if (update) {
            paakrevdGodkjenningskursRegistrationRepository.update(paakrevdGodkjenningskursRegistration.toEntity())
        } else {
            paakrevdGodkjenningskursRegistrationRepository.save(paakrevdGodkjenningskursRegistration.toEntity())
        }
        val dto = saved.toDTO()
        paakrevdGodkjenningskursEventHandler.queueDTORapidEvent(dto, eventName = EventName.registeredPaakrevdGodkjenningskursV1)
        return dto
    }
}
