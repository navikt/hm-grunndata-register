package no.nav.hm.grunndata.register.catalog

import jakarta.inject.Singleton

@Singleton
class CatalogImportService(private val catalogImportRepository: CatalogImportRepository) {

    suspend fun findByOrderRef(orderRef: String): List<CatalogImport> {
        return catalogImportRepository.findByOrderRef(orderRef)
    }

    suspend fun persistCatalog(catalogImportList: List<CatalogImport>): CatalogImportResult {
        val updatedList = mutableListOf<CatalogImport>()
        val deactivatedList = mutableListOf<CatalogImport>()
        val insertedList = mutableListOf<CatalogImport>()
        val existingCatalog = catalogImportRepository.findByOrderRef(catalogImportList.first().orderRef)
        if (existingCatalog.isEmpty()) {
            insertedList.addAll(catalogImportList)
        }
        catalogImportList.forEach { catalogImport ->
            val existing = existingCatalog.find { it.supplierRef == catalogImport.supplierRef }
            if (existing == null) {
                insertedList.add(catalogImport)
            } 
        }

        updatedList.forEach { catalogImportRepository.update(it) }
        insertedList.forEach { catalogImportRepository.save(it) }

        return CatalogImportResult(updatedList, deactivatedList, insertedList)
    }

}

data class CatalogImportResult(
    val updatedList: List<CatalogImport>,
    val deactivatedList: List<CatalogImport>,
    val insertedList: List<CatalogImport>
)