package no.nav.hm.grunndata.register.catalog

import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationService
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationService

@Singleton
open class CatalogImportService(private val catalogImportRepository: CatalogImportRepository,
                                private val agreementRegistrationService: AgreementRegistrationService
) {

    suspend fun findByOrderRef(orderRef: String): List<CatalogImport> {
        return catalogImportRepository.findByOrderRef(orderRef)
    }


    @Transactional
    open suspend fun prepareCatalogImportResult(catalogImportList: List<CatalogImport>, forceUpdate: Boolean): CatalogImportResult {
        val updatedList = mutableListOf<CatalogImport>()
        val insertedList = mutableListOf<CatalogImport>()
        val orderRef = catalogImportList.first().orderRef
        val existingCatalog = catalogImportRepository.findByOrderRef(orderRef)
        if (existingCatalog.isEmpty()) {
            insertedList.addAll(catalogImportList)
        } else {
            catalogImportList.forEach { catalogImport ->
                val existing = existingCatalog.find { it.supplierRef == catalogImport.supplierRef }
                if (existing == null) {
                    insertedList.add(catalogImport)
                } else if (forceUpdate || existing != catalogImport) {
                    updatedList.add(catalogImport.copy(id = existing.id, created = existing.created))
                }
            }
        }
        val deactivatedList = existingCatalog.filter { it.supplierRef !in catalogImportList.map { c -> c.supplierRef } }.map {
            it.copy( dateTo = LocalDate.now().minusDays(1), updated = LocalDateTime.now())
        }
        return CatalogImportResult(updatedList, deactivatedList, insertedList)
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