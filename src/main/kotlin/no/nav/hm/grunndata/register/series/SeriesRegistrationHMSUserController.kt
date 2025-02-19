package no.nav.hm.grunndata.register.series

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory
import java.util.UUID


@Secured(Roles.ROLE_HMS)
@Controller(SeriesRegistrationHmsUserController.API_V1_SERIES)
@Tag(name = "Hms Series")
class SeriesRegistrationHmsUserController(
    private val seriesRegistrationService: SeriesRegistrationService,
    private val seriesDTOMapper: SeriesDTOMapper,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesRegistrationHmsUserController::class.java)
        const val API_V1_SERIES = "/hms/api/v1/series"
    }

    @Get("/{id}")
    suspend fun readSeries(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<SeriesDTO> {
        return seriesRegistrationService.findById(id, authentication)?.let {
            HttpResponse.ok(seriesDTOMapper.toDTOV2(it))
        } ?: run {
            LOG.warn("Series with id $id does not exist")
            HttpResponse.notFound()
        }
    }
}

