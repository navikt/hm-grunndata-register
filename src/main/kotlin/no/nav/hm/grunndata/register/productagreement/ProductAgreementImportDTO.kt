package no.nav.hm.grunndata.register.productagreement

import io.micronaut.core.annotation.Introspected
import no.nav.hm.grunndata.register.catalog.CatalogImport


@Introspected
data class CatalogImportResultReport(
    val file: String,
    val rows: Int,
    val supplier: String,
    val insertedList: List<CatalogImport> = emptyList(),
    val updatedList: List<CatalogImport> = emptyList(),
    val deactivatedList: List<CatalogImport> = emptyList(),
)



