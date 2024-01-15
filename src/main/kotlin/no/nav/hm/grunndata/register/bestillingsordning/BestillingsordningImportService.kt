package no.nav.hm.grunndata.register.bestillingsordning

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import java.net.URL





data class BestillingsordningDTO(
    val hmsnr: String,
    val navn: String
)

@Singleton
class BestillingsordningImportService(
    @Value("\${bestillingsordning.url}")
    private val url : String,
    private val objectMapper: ObjectMapper) {

    private val boMap: Map<String, BestillingsordningDTO> =
        objectMapper.readValue(URL(url), object : TypeReference<List<BestillingsordningDTO>>(){}).associateBy { it.hmsnr }

    fun isBestillingsordning(hmsnr: String): Boolean = boMap.containsKey(hmsnr)

    fun getBestillingsorning(hmsnr: String): BestillingsordningDTO? = boMap[hmsnr]

}