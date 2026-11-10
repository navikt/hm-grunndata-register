package no.nav.hm.grunndata.register.iso.updatev22

import no.nav.hm.grunndata.register.logging.sanitizeForLog
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import no.nav.hm.grunndata.register.iso.v22.IsoMapper
import no.nav.hm.grunndata.register.techlabel.TechLabelRegistrationRepository

@Hidden
@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/iso22-util/techlabel/update")
class TechLabelIso22UpdateController(val techLabelRepository: TechLabelRegistrationRepository,
                                     val isoMapper: IsoMapper) {

    @Put("/mapIso22")
    suspend fun mapIso22() {
        mapIsoCode16ToIsoCode22()
    }

    suspend fun mapIsoCode16ToIsoCode22() {
        val techLabels = techLabelRepository.findAll()
        techLabels.collect { techlabel ->
            isoMapper.mapIso16To22(techlabel.isoCode)?.let { iso22 ->
                if (iso22.verified) {
                    techLabelRepository.update(techlabel.copy(isoCode22 = iso22.code22))
                }
                else {
                    LOG.warn("Found mapping for {} to iso22 {} but not verified, skipping update", techlabel.isoCode.sanitizeForLog(), iso22.code22.sanitizeForLog())
                }
            } ?: run {
                LOG.warn("Could not find mapping for {}", techlabel.isoCode.sanitizeForLog())
            }
        }
    }

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(TechLabelIso22UpdateController::class.java)
    }
}