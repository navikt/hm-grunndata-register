package no.nav.hm.grunndata.register.series

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.jpa.criteria.impl.LiteralExpression
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.data.runtime.criteria.where
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.security.Roles
import java.util.UUID

@Secured(Roles.ROLE_ADMIN)
@Controller(SeriesAdminController.API_V1_SERIES)
class SeriesAdminController(private val seriesRegistrationService: SeriesRegistrationService) {
    companion object {
        const val API_V1_SERIES = "/admin/api/v1/series"
    }

    @Get("/{?params*}")
    suspend fun getSeries(
        @QueryValue params: HashMap<String, String>?,
        pageable: Pageable,
        authentication: Authentication,
    ): Page<SeriesRegistrationDTO> {
        return seriesRegistrationService.findAll(buildCriteriaSpec(params), pageable)
    }

    private fun buildCriteriaSpec(params: java.util.HashMap<String, String>?): PredicateSpecification<SeriesRegistration>? =
        params?.let {
            where {
                if (params.contains("adminStatus")) root[SeriesRegistration::adminStatus] eq AdminStatus.valueOf(params["adminStatus"]!!)
                if (params.contains("status")) {
                    val statusList: List<SeriesStatus> =
                        params["status"]!!.split(",").map { SeriesStatus.valueOf(it) }
                    root[SeriesRegistration::status] inList statusList
                }
                if (params.contains("supplierId")) root[SeriesRegistration::supplierId] eq UUID.fromString(params["supplierId"]!!)
                if (params.contains("draft")) root[SeriesRegistration::draftStatus] eq DraftStatus.valueOf(params["draft"]!!)
                if (params.contains("createdByUser")) root[SeriesRegistration::createdByUser] eq params["createdByUser"]
                if (params.contains("updatedByUser")) root[SeriesRegistration::updatedByUser] eq params["updatedByUser"]
                if (params.contains("excludedStatus")) {
                    root[SeriesRegistration::status] ne params["excludedStatus"]
                }
            }.and { root, criteriaBuilder ->
                if (params.contains("title")) {

                    criteriaBuilder.like(
                        root[SeriesRegistration::title],
                        LiteralExpression("%${params["title"]?.replaceFirstChar(Char::titlecase)}%"),
                    )
                    criteriaBuilder.like(root[SeriesRegistration::title], LiteralExpression("%${params["title"]}%"))
                } else {
                    null
                }
            }
        }
}
