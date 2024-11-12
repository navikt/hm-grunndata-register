package no.nav.hm.grunndata.register.series.version

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.RequestBean
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.product.version.ProductRegistrationVersionService
import no.nav.hm.grunndata.register.runtime.where
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.version.Difference
import org.slf4j.LoggerFactory


@Secured(Roles.ROLE_ADMIN)
@Controller(SeriesRegistrationVersionAdminController.API_V1_SERIES_VERSIONS)
@Tag(name = "Admin Series")
class SeriesRegistrationVersionAdminController(
    private val seriesRegistrationVersionService: SeriesRegistrationVersionService,
    private val productRegistrationVersionService: ProductRegistrationVersionService
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesRegistrationVersionAdminController::class.java)
        const val API_V1_SERIES_VERSIONS = "/admin/api/v1/series/versions"
    }

    @Get("/")
    suspend fun getSeriesVersions(
        @RequestBean seriesVersionCriteria: SeriesVersionCriteria,
        pageable: Pageable,
        authentication: Authentication,
    ): Page<SeriesRegistrationVersionDTO> {
        return seriesRegistrationVersionService.findAll(buildCriteriaSpec(seriesVersionCriteria), pageable).map { it.toDTO() }
    }

    private fun buildCriteriaSpec(criteria: SeriesVersionCriteria): PredicateSpecification<SeriesRegistrationVersion>? =
        if (criteria.isNotEmpty()) {
            where {
                if (criteria.seriesId != null ) root[SeriesRegistrationVersion::seriesId] eq criteria.seriesId
                if (criteria.version != null) root[SeriesRegistrationVersion::version] eq criteria.version
                if (criteria.status != null) root[SeriesRegistrationVersion::status] eq criteria.status
                if (criteria.adminStatus != null) root[SeriesRegistrationVersion::adminStatus] eq criteria.adminStatus
                if (criteria.draftStatus != null) root[SeriesRegistrationVersion::draftStatus] eq criteria.draftStatus
            }
        } else null

    @Get("/{seriesId}/compare/{version}/approved")
    suspend fun compareVersionWithApproved(
        seriesId: UUID,
        version: Long,
        authentication: Authentication,
    ): HttpResponse<Difference<String, Any>> {
        val seriesVersion = seriesRegistrationVersionService.findBySeriesIdAndVersion(seriesId, version) ?: return HttpResponse.notFound()
        return HttpResponse.ok(seriesRegistrationVersionService.diffWithLastApprovedVersion(seriesVersion))
    }

}

@Introspected
data class SeriesVersionCriteria(
    val seriesId: UUID? = null,
    val version: Long? = null,
    val status: SeriesStatus? = null,
    val adminStatus: AdminStatus? = null,
    val draftStatus: DraftStatus? = null,
) {
    fun isNotEmpty() = seriesId != null || version != null || status != null
            || adminStatus != null || draftStatus != null
}