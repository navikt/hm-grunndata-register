package no.nav.hm.grunndata.register.catalog

import jakarta.inject.Singleton
import jakarta.transaction.Transactional

@Singleton
open class CatalogImportService(private val catalogImportRepository: CatalogImportRepository) {

    suspend fun findByOrderRef(orderRef: String): List<CatalogImport> {
        return catalogImportRepository.findByOrderRef(orderRef)
    }


    @Transactional
    open suspend fun createCatalogImportResult(catalogImportList: List<CatalogImport>): CatalogImportResult {
        val updatedList = mutableListOf<CatalogImport>()
        val insertedList = mutableListOf<CatalogImport>()
        val existingCatalog = catalogImportRepository.findByOrderRef(catalogImportList.first().orderRef)
        if (existingCatalog.isEmpty()) {
            insertedList.addAll(catalogImportList)
        } else {
            catalogImportList.forEach { catalogImport ->
                val existing = existingCatalog.find { it.supplierRef == catalogImport.supplierRef }
                if (existing == null) {
                    insertedList.add(catalogImport)
                } else {
                    updatedList.add(catalogImport.copy(id = existing.id))
                }

            }
        }
        val deactivatedList = existingCatalog.filter { it.supplierRef !in catalogImportList.map { c -> c.supplierRef } }
        return CatalogImportResult(updatedList, deactivatedList, insertedList)
    }

    @Transactional
    open suspend fun persistCatalogImportResult(catalogImportResult: CatalogImportResult) {
        catalogImportResult.updatedList.forEach { catalogImportRepository.update(it) }
        catalogImportResult.deactivatedList.forEach { catalogImportRepository.delete(it) }
        catalogImportResult.insertedList.forEach { catalogImportRepository.save(it) }
    }
}

data class CatalogImportResult(
    val updatedList: List<CatalogImport>,
    val deactivatedList: List<CatalogImport>,
    val insertedList: List<CatalogImport>
)