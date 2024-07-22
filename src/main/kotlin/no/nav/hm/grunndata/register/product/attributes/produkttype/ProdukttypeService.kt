package no.nav.hm.grunndata.register.product.attributes.produkttype

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.rapid.dto.ProdukttypeStatus
import no.nav.hm.grunndata.rapid.event.EventName
import org.slf4j.LoggerFactory
import java.net.URL
import java.time.LocalDateTime

data class ProdukttypeDTO(
    val isokode: String,
    val produkttype: String,
)

@Singleton
open class ProdukttypeService(
    @Value("\${digihotSortiment.produkttype}")
    private val url : String,
    private val objectMapper: ObjectMapper,
    private val produkttypeRegistrationRepository: ProdukttypeRegistrationRepository,
    private val produkttypeEventHandler: ProdukttypeEventHandler,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(ProdukttypeService::class.java)
    }

    suspend fun importAndUpdateDb() {
        val boMap = objectMapper.readValue(URL(url), object : TypeReference<List<ProdukttypeDTO>>(){}).associateBy { it.isokode }.mapValues { it.value.produkttype }

        val deactiveList = produkttypeRegistrationRepository.findByStatus(ProdukttypeStatus.ACTIVE).filter { currentlyActive ->
            return@filter !boMap.containsKey(currentlyActive.isokode)
        }

        boMap.forEach { (isokode, produkttype) ->
            produkttypeRegistrationRepository.findByIsokode(isokode)?.let { existing ->
                if (existing.status != ProdukttypeStatus.ACTIVE || existing.produkttype != produkttype) {
                    // update produkttype which was previously deactivated
                    LOG.info("Updating produkttype for isokode: ${isokode}: $produkttype")
                    saveAndCreateEvent(existing.copy (
                        produkttype = produkttype,
                        status = ProdukttypeStatus.ACTIVE,
                        updated = LocalDateTime.now(),
                        deactivated = null,
                        updatedByUser = "system",
                    ).toDTO(), update = true)
                }
                existing
            } ?: run {
                // new produkttype
                LOG.info("New produkttype for isokode: ${isokode}: $produkttype")
                val produkttypeRegistration = ProdukttypeRegistration(isokode = isokode, produkttype = produkttype)
                saveAndCreateEvent(produkttypeRegistration.toDTO(), update = false)
            }
        }

        deactiveList.forEach {
            LOG.info("Deactivate produkttype for isokode: ${it.isokode}: ${it.produkttype}")
            saveAndCreateEvent(it.copy(status = ProdukttypeStatus.INACTIVE,
                updated = LocalDateTime.now(), deactivated = LocalDateTime.now()).toDTO(), update = true)
        }
    }

    @Transactional
    open suspend fun saveAndCreateEvent(produkttypeRegistration: ProdukttypeRegistrationDTO, update:Boolean): ProdukttypeRegistrationDTO {
        val saved = if (update) {
            produkttypeRegistrationRepository.update(produkttypeRegistration.toEntity())
        } else {
            produkttypeRegistrationRepository.save(produkttypeRegistration.toEntity())
        }
        val dto = saved.toDTO()
        produkttypeEventHandler.queueDTORapidEvent(dto, eventName = EventName.registeredProdukttypeV1)
        return dto
    }
}
