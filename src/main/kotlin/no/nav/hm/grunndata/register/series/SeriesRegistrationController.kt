package no.nav.hm.grunndata.register.series

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.supplierId
import org.slf4j.LoggerFactory
import java.util.UUID

@Secured(Roles.ROLE_SUPPLIER)
@Controller(SeriesRegistrationController.API_V1_SERIES)
@Tag(name = "Vendor Series")
class SeriesRegistrationController(
    private val seriesRegistrationService: SeriesRegistrationService,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesRegistrationController::class.java)
        const val API_V1_SERIES = "/vendor/api/v1/series"
    }

    @Put("/request-approval/{seriesUUID}")
    suspend fun requestApproval(
        @PathVariable seriesUUID: UUID,
        authentication: Authentication,
    ): HttpResponse<Any> {
        val seriesToUpdate = seriesRegistrationService.findById(seriesUUID) ?: return HttpResponse.notFound()

        if (seriesToUpdate.supplierId != authentication.supplierId()) {
            LOG.warn("SupplierId in request does not match authenticated supplierId")
            return HttpResponse.unauthorized()
        }

        if (seriesToUpdate.draftStatus != DraftStatus.DRAFT) throw BadRequestException("series is marked as done")

        seriesRegistrationService.requestApprovalForSeriesAndVariants(seriesToUpdate)

        LOG.info("set series to pending approval: $seriesUUID")
        return HttpResponse.ok()
    }
}

data class SeriesDraftResponse(val id: UUID)

data class SeriesDraftWithDTO(val title: String, val isoCategory: String)
