package no.nav.hm.grunndata.register.catalog

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.RequestBean
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import no.nav.hm.grunndata.register.runtime.get
import no.nav.hm.grunndata.register.runtime.where

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/catalog/import")
@Hidden
class CatalogImportController(private val catalogImportRepository: CatalogImportRepository) {

    @Get("/")
    suspend fun fetchCatalogImport(
        @RequestBean criteria: CatalogImportCriteria,
        pageable: Pageable
    ): Page<CatalogImport> {
        return catalogImportRepository.findAll(buildCriteriaSpec(criteria), pageable)
    }

    private fun buildCriteriaSpec(criteria: CatalogImportCriteria): PredicateSpecification<CatalogImport>? =
        if (criteria.isNotEmpty()) {
            where {
                criteria.orderRef?.let { root[CatalogImport::orderRef] eq it}
            }
        } else null


}

@Introspected
data class CatalogImportCriteria(
    val orderRef: String? = null
) {
    fun isNotEmpty() = orderRef != null
}
