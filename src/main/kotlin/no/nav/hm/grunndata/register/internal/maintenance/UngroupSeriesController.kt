package no.nav.hm.grunndata.register.internal.maintenance

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.series.SeriesRegistrationService
import org.slf4j.LoggerFactory
import java.util.UUID

@Controller("/internal/fix/series")
@Hidden
@Secured(SecurityRule.IS_ANONYMOUS)
class UngroupSeriesController(private val seriesRegistrationService: SeriesRegistrationService) {


    @Post("/ungroup/{seriesId}")
    suspend fun ungroupSeries(seriesId: UUID) {
        val seriesToUngroup = seriesRegistrationService.findById(seriesId) ?: throw BadRequestException("Series with id $seriesId not found")
        LOG.info("Ungrouping series: $seriesId")
        seriesRegistrationService.ungroupSeries(seriesToUngroup)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(UngroupSeriesController::class.java)
    }
}