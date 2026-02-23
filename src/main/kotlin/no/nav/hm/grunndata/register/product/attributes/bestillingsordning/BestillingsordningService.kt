package no.nav.hm.grunndata.register.product.attributes.bestillingsordning

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.rapid.dto.BestillingsordningStatus
import no.nav.hm.grunndata.rapid.event.EventName
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDateTime

data class BestillingsordningDTO(
    val hmsnr: String,
    val navn: String
)

@Singleton
open class BestillingsordningService(
    @Value("\${digihotSortiment.bestillingsordning}")
    private val url : String,
    private val objectMapper: ObjectMapper,
    private val bestillingsordningRegistrationRepository: BestillingsordningRegistrationRepository,
    private val bestillingsordningEventHandler: BestillingsordningEventHandler,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(BestillingsordningService::class.java)
    }

    private fun loadBoMap(): Map<String, BestillingsordningDTO> {
        val reader = objectMapper.readerFor(object : TypeReference<List<BestillingsordningDTO>>() {})
        val list: List<BestillingsordningDTO> = reader.readValue(URI(url).toURL())
        return list.associateBy { it.hmsnr }
    }

    private var boMap: Map<String, BestillingsordningDTO> = loadBoMap()

    suspend fun findByHmsArtNr(hmsArtNr: String): BestillingsordningRegistrationDTO? = bestillingsordningRegistrationRepository.findByHmsArtNr(hmsArtNr)?.toDTO()

    suspend fun importAndUpdateDb() {
        boMap = loadBoMap()
        val deactiveList = bestillingsordningRegistrationRepository.findByStatus(BestillingsordningStatus.ACTIVE).filter {
            !boMap.containsKey(it.hmsArtNr)
        }

        boMap.forEach { (hmsnr, bestillingsordningDTO) ->
            bestillingsordningRegistrationRepository.findByHmsArtNr(hmsnr)?.let { existing ->
                if (existing.status != BestillingsordningStatus.ACTIVE) {
                    // update bestillingsordning which was previously deactivated
                    LOG.info("Updating bestillingsordning for hmsnr: $hmsnr")
                    saveAndCreateEvent(existing.copy (
                        status = BestillingsordningStatus.ACTIVE,
                        updated = LocalDateTime.now(),
                        deactivated = null,
                        updatedByUser = "system",
                    ).toDTO(), update = true)
                }
                existing
            } ?: run {
                // new bestillingsordning
                LOG.info("New bestillingsordning for hmsnr: $hmsnr")
                val bestillingsordningRegistration = BestillingsordningRegistration(hmsArtNr = hmsnr, navn = bestillingsordningDTO.navn)
                saveAndCreateEvent(bestillingsordningRegistration.toDTO(), update = false)
            }

        }

        deactiveList.forEach {
            LOG.info("Deactivate bestillingsordning for hmsnr: ${it.hmsArtNr}")
            saveAndCreateEvent(it.copy(status = BestillingsordningStatus.INACTIVE,
                updated = LocalDateTime.now(), deactivated = LocalDateTime.now()).toDTO(), update = true)
        }
    }

    @Transactional
    open suspend fun saveAndCreateEvent(bestillingsordningRegistration: BestillingsordningRegistrationDTO, update:Boolean): BestillingsordningRegistrationDTO {
        val saved = if (update) {
            bestillingsordningRegistrationRepository.update(bestillingsordningRegistration.toEntity())
        } else {
            bestillingsordningRegistrationRepository.save(bestillingsordningRegistration.toEntity())
        }
        val dto = saved.toDTO()
        bestillingsordningEventHandler.queueDTORapidEvent(dto, eventName = EventName.registeredBestillingsordningV1)
        return dto
    }
}
