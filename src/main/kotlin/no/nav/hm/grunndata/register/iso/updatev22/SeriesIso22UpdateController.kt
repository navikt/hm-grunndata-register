package no.nav.hm.grunndata.register.iso.updatev22

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import no.nav.hm.grunndata.register.iso.v22.IsoMapper
import no.nav.hm.grunndata.register.iso.v22.getLevelFromIsoCode
import no.nav.hm.grunndata.register.iso.v22.isOebsCategory
import no.nav.hm.grunndata.register.series.SeriesRegistrationRepository

@Hidden
@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/iso22-util/series/update")
class SeriesIso22UpdateController(private val seriesRegistrationRepository: SeriesRegistrationRepository,
                                  private val isoMapper: IsoMapper) {

    @Put("/mapIso22")
    suspend fun mapIso22() {
        mapIsoCode16ToIsoCode22()
    }

    suspend fun mapIsoCode16ToIsoCode22() {
        val series = seriesRegistrationRepository.findAll()
        series.collect { serie ->
            isoMapper.mapIso16To22(serie.isoCategory)?.let { iso22 ->
                if (iso22.verified && iso22.code22 != null &&
                    (getLevelFromIsoCode(iso22.code22) == 4 || isOebsCategory(iso22.code22))) {
                        seriesRegistrationRepository.update(serie.copy(isoCategory22 = iso22.code22))
                }
                else {
                    LOG.warn("Found mapping for ${serie.isoCategory} to iso22 ${iso22.code22} but not verified, skipping update")
                }
            } ?: run {
                LOG.warn("Could not find mapping for ${serie.isoCategory}")
            }
        }
    }

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(SeriesIso22UpdateController::class.java)
    }
}