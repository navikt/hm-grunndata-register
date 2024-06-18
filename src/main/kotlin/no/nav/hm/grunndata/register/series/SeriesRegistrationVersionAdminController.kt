package no.nav.hm.grunndata.register.series

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.data.runtime.criteria.where
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory


@Secured(Roles.ROLE_ADMIN)
@Controller(SeriesRegistrationVersionAdminController.API_V1_SERIES_VERSIONS)
@Tag(name = "Admin Series")
class SeriesRegistrationVersionAdminController(private val seriesRegistrationVersionService: SeriesRegistrationVersionService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesRegistrationVersionAdminController::class.java)
        const val API_V1_SERIES_VERSIONS = "/admin/api/v1/series/versions"
    }

    @Get("/{?params*}")
    suspend fun getSeriesVersions(
        @QueryValue params: HashMap<String, String>?,
        pageable: Pageable,
        authentication: Authentication,
    ): Page<SeriesRegistrationVersionDTO> {
        return seriesRegistrationVersionService.findAll(buildCriteriaSpec(params), pageable)
    }

    private fun buildCriteriaSpec(params: HashMap<String, String>?): PredicateSpecification<SeriesRegistrationVersion>? =
        params?.let {
            where {
                if (it.containsKey("seriesId")) root[SeriesRegistrationVersion::seriesId] eq it["seriesId"]
                if (it.containsKey("version")) root[SeriesRegistrationVersion::version] eq it["version"]
                if (it.containsKey("status")) root[SeriesRegistrationVersion::status] eq SeriesStatus.valueOf(it["status"]!!)
                if (it.containsKey("adminStatus")) root[SeriesRegistrationVersion::adminStatus] eq AdminStatus.valueOf(it["adminStatus"]!!)
                if (it.containsKey("draftStatus")) root[SeriesRegistrationVersion::draftStatus] eq DraftStatus.valueOf(it["draftStatus"]!!)
            }
        }



}