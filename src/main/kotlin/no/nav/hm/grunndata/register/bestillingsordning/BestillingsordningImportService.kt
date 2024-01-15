package no.nav.hm.grunndata.register.bestillingsordning

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import java.net.URL
import java.time.LocalDateTime


data class BestillingsordningDTO(
    val hmsnr: String,
    val navn: String
)

@Singleton
class BestillingsordningImportService(
    @Value("\${bestillingsordning.url}")
    private val url : String,
    private val objectMapper: ObjectMapper, private val bestillingsordningRegistrationRepository: BestillingsordningRegistrationRepository) {

    private var boMap: Map<String, BestillingsordningDTO> =
        objectMapper.readValue(URL(url), object : TypeReference<List<BestillingsordningDTO>>(){}).associateBy { it.hmsnr }

    fun isBestillingsordning(hmsnr: String): Boolean = boMap.containsKey(hmsnr)

    fun getBestillingsorning(hmsnr: String): BestillingsordningDTO? = boMap[hmsnr]

    suspend fun importAndUpdateDb() {
        boMap = objectMapper.readValue(URL(url), object : TypeReference<List<BestillingsordningDTO>>(){}).associateBy { it.hmsnr }
        var newList : MutableList<BestillingsordningRegistration> = mutableListOf()
        val deactiveList = bestillingsordningRegistrationRepository.findByStatus(BestillingsordningStatus.ACTIVE).filter {
            !boMap.containsKey(it.hmsArtNr)
        }
        boMap.forEach { (hmsnr, bestillingsordningDTO) ->
            newList.add(bestillingsordningRegistrationRepository.findByHmsArtNr(hmsnr) ?: run {
                val bestillingsordningRegistration = BestillingsordningRegistration(hmsArtNr = hmsnr, navn = bestillingsordningDTO.navn)
                bestillingsordningRegistrationRepository.save(bestillingsordningRegistration)
            })
            // TODO send event
        }
        deactiveList.forEach {
            bestillingsordningRegistrationRepository.update(it.copy(status = BestillingsordningStatus.INACTIVE,
                updated = LocalDateTime.now(), deactivated = LocalDateTime.now()))
            // TODO send event
        }
    }

}