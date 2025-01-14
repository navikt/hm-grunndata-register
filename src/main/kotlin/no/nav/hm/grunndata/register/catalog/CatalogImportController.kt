package no.nav.hm.grunndata.register.catalog

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
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
    suspend fun fetchCatalogSeriesInfo(
        @QueryValue orderRef: String,
        pageable: Pageable
    ): List<CatalogSeriesInfo> {
        return catalogImportRepository.findCatalogSeriesInfoByOrderRef(orderRef)
    }

}