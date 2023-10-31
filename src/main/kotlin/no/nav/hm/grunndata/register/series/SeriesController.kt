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
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory

@Secured(Roles.ROLE_SUPPLIER)
@Controller(SeriesController.API_V1_SERIES)
class SeriesController(private val seriesRegistrationService: SeriesRegistrationService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesController::class.java)
        const val API_V1_SERIES = "/vendor/api/v1/series"
    }

    @Get("/{?params*}")
    suspend fun getSeries(
        @QueryValue params: HashMap<String, String>?,
        pageable: Pageable,
        authentication: Authentication
    ): Page<SeriesRegistrationDTO> {
        return seriesRegistrationService.findAll(buildCriteriaSpec(params), pageable)
    }

    private fun buildCriteriaSpec(params: HashMap<String, String>?): PredicateSpecification<SeriesRegistration>? =
        params?.let {
            where {
                if (params.contains("supplierId")) root[SeriesRegistration::supplierId] eq params["supplierId"]
            }.and { root, criteriaBuilder ->
                if (params.contains("title")) criteriaBuilder.like(
                    root[SeriesRegistration::title],
                    params["title"]
                ) else null
            }
        }
}