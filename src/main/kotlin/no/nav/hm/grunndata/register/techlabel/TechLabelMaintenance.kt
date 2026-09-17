package no.nav.hm.grunndata.register.techlabel

import io.micronaut.data.runtime.criteria.get
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.filter
import no.nav.hm.grunndata.register.iso.v22.IsoMapper
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.runtime.where
import org.slf4j.LoggerFactory
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper

@Singleton
class TechLabelMaintenance(
    val techLabelRepository: TechLabelRegistrationRepository,
    val isoMapper: IsoMapper,
    val productRegistrationService: ProductRegistrationService,
    val objectMapper: ObjectMapper,
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(TechLabelMaintenance::class.java)
    }

    suspend fun mapIsoCode16ToIsoCode22() {
        val techLabels = techLabelRepository.findAll()
        techLabels.collect { techlabel ->
            isoMapper.mapIso16To22(techlabel.isoCode)?.let { iso22 ->
                if (iso22.verified) {
                    techLabelRepository.update(techlabel.copy(isoCode22 = iso22.code22))
                }
                else {
                    LOG.warn("Found mapping for ${techlabel.isoCode} to iso22 ${iso22.code22} but not verified, skipping update")
                }
            } ?: run {
                LOG.warn("Could not find mapping for ${techlabel.isoCode}")
            }
        }
    }
}

data class TechLabelMapping(
    val original: String,
    val normalized: String,
    val category: String
)