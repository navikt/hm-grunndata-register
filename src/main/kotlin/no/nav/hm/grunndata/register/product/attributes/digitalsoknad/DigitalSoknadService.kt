package no.nav.hm.grunndata.register.product.attributes.digitalsoknad

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.rapid.dto.DigitalSoknadStatus
import no.nav.hm.grunndata.rapid.event.EventName
import org.slf4j.LoggerFactory
import java.net.URL
import java.time.LocalDateTime

data class DigitalSoknadDTO(
    val hmsnr: String,
)

@Singleton
open class DigitalSoknadService(
    @Value("\${digitalSoknad.url}")
    private val url : String,
    private val objectMapper: ObjectMapper,
    private val digitalSoknadRegistrationRepository: DigitalSoknadRegistrationRepository,
    private val digitalSoknadEventHandler: DigitalSoknadEventHandler,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(DigitalSoknadService::class.java)
    }

    private var boMap: Map<String, DigitalSoknadDTO> =
        objectMapper.readValue(URL(url), object : TypeReference<List<DigitalSoknadDTO>>(){}).associateBy { it.hmsnr }

    suspend fun findByHmsArtNr(hmsArtNr: String): DigitalSoknadRegistrationDTO? = digitalSoknadRegistrationRepository.findByHmsArtNr(hmsArtNr)?.toDTO()

    suspend fun importAndUpdateDb() {
        boMap = objectMapper.readValue(URL(url), object : TypeReference<List<DigitalSoknadDTO>>(){}).associateBy { it.hmsnr }
        val deactiveList = digitalSoknadRegistrationRepository.findByStatus(DigitalSoknadStatus.ACTIVE).filter {
            !boMap.containsKey(it.hmsArtNr)
        }
        boMap.forEach { (hmsnr, bestillingsordningDTO) ->
            digitalSoknadRegistrationRepository.findByHmsArtNr(hmsnr) ?: run {
                // new digital soknad
                LOG.info("New digital soknad for $hmsnr")
                val bestillingsordningRegistration = DigitalSoknadRegistration(hmsArtNr = hmsnr)
                saveAndCreateEvent(bestillingsordningRegistration.toDTO(), update = false)
            }

        }
        deactiveList.forEach {
            LOG.info("Deactivate digital soknad for ${it.hmsArtNr}")
            saveAndCreateEvent(it.copy(status = DigitalSoknadStatus.INACTIVE,
                updated = LocalDateTime.now(), deactivated = LocalDateTime.now()).toDTO(), update = true)
        }
    }

    @Transactional
    open suspend fun saveAndCreateEvent(digitalSoknadRegistration: DigitalSoknadRegistrationDTO, update:Boolean): DigitalSoknadRegistrationDTO {
        val saved = if (update) {
            digitalSoknadRegistrationRepository.update(digitalSoknadRegistration.toEntity())
        } else {
            digitalSoknadRegistrationRepository.save(digitalSoknadRegistration.toEntity())
        }
        val dto = saved.toDTO()
        digitalSoknadEventHandler.queueDTORapidEvent(dto, eventName = EventName.registeredDigitalSoknadV1)
        return dto
    }
}
