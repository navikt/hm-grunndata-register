package no.nav.hm.grunndata.register.catalog

import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Singleton
open class CatalogImportService(private val catalogImportRepository: CatalogImportRepository) {

    suspend fun findByOrderRef(orderRef: String): List<CatalogImport> {
        return catalogImportRepository.findByOrderRef(orderRef)
    }


    @Transactional
    open suspend fun prepareCatalogImportResult(catalogImportList: List<CatalogImport>): CatalogImportResult {
        val updatedList = mutableListOf<CatalogImport>()
        val insertedList = mutableListOf<CatalogImport>()
        verifyCatalogImportList(catalogImportList)
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
        val deactivatedList = existingCatalog.filter { it.supplierRef !in catalogImportList.map { c -> c.supplierRef } }.map {
            it.copy( dateTo = LocalDate.now().minusDays(1), updated = LocalDateTime.now())
        }
        return CatalogImportResult(updatedList, deactivatedList, insertedList)
    }

    private fun verifyCatalogImportList(catalogImportList: List<CatalogImport>) {
        if (catalogImportList.isEmpty()) {
            throw IllegalArgumentException("Catalog import list is empty")
        }
        if (catalogImportList.map { it.orderRef }.distinct().size > 1) {
            throw IllegalArgumentException("Ugyldig katalog, inneholder flere bestillingsnr")
        }
        if (catalogImportList.map { it.supplierName }.distinct().size > 1) {
            throw IllegalArgumentException("Ugyldig katalog, inneholder flere leverandører")
        }
        if (catalogImportList.map { it.reference }.distinct().size > 1) {
            throw IllegalArgumentException("Ugylding katalog, inneholder flere rammeavtale referansenr")
        }
    }

    @Transactional
    open suspend fun persistCatalogImportResult(catalogImportResult: CatalogImportResult) {
        catalogImportResult.updatedList.forEach { catalogImportRepository.update(it) }
        catalogImportResult.deactivatedList.forEach { catalogImportRepository.update(it) }
        catalogImportResult.insertedList.forEach { catalogImportRepository.save(it) }
    }
}

data class CatalogImportResult(
    val updatedList: List<CatalogImport>,
    val deactivatedList: List<CatalogImport>,
    val insertedList: List<CatalogImport>
)