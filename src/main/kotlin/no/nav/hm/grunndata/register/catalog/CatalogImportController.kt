package no.nav.hm.grunndata.register.catalog


import io.micronaut.data.model.Pageable
import io.micronaut.data.model.Slice
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden


@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/catalog/import")
@Hidden
class CatalogImportController(
    private val catalogImportRepository: CatalogImportRepository,
    private val catalogFileRepository: CatalogFileRepository
) {

    @Get("/")
    suspend fun fetchCatalogSeriesInfo(
        @QueryValue orderRef: String
    ): List<CatalogSeriesInfo> {
        return catalogImportRepository.findCatalogSeriesInfoByOrderRef(orderRef)
    }

    @Get("/files")
    suspend fun fetchAllCatalogFiles(
        pageable: Pageable,
    ): Slice<CatalogFileDTO> = catalogFileRepository.findMany(pageable)

}