package no.nav.hm.grunndata.register.catalog

import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.rapid.dto.AgreementStatus
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationService
import no.nav.hm.grunndata.register.error.BadRequestException
import java.util.UUID

@Singleton
open class CatalogImportService(private val catalogImportRepository: CatalogImportRepository,
                                private val agreementRegistrationService: AgreementRegistrationService
) {

    suspend fun findByOrderRef(orderRef: String): List<CatalogImport> {
        return catalogImportRepository.findByOrderRef(orderRef)
    }


    @Transactional
    open suspend fun convertAndCreateCatalogImportResult(catalogImportList: List<CatalogImport>, forceUpdate: Boolean): CatalogImportResult {
        val updatedList = mutableListOf<CatalogImport>()
        val insertedList = mutableListOf<CatalogImport>()
        val orderRef = catalogImportList.first().orderRef
        val existingCatalog = catalogImportRepository.findByOrderRef(orderRef)
        if (existingCatalog.isEmpty()) {
            insertedList.addAll(catalogImportList)
        } else {
            catalogImportList.forEach { catalogImport ->
                val existing = existingCatalog.find { it.hmsArtNr== catalogImport.hmsArtNr }
                if (existing == null) {
                    insertedList.add(catalogImport)
                } else if (forceUpdate || existing != catalogImport) {
                    updatedList.add(catalogImport.copy(id = existing.id, created = existing.created))
                }
            }
        }
        val deactivatedList = existingCatalog.filter { it.hmsArtNr !in catalogImportList.map { c -> c.hmsArtNr } }.map {
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

    suspend fun mapExcelDTOToCatalogImportResult(
        importedExcelCatalog: List<CatalogImportExcelDTO>,
        supplierId: UUID,
        forceUpdate: Boolean,
    ): CatalogImportResult {
        verifyCatalogImportList(importedExcelCatalog)
        val agreementRef = importedExcelCatalog.first().reference
        val cleanRef = agreementRef.lowercase().replace("/", "-")
        val agreement = agreementRegistrationService.findByReferenceLike("%$cleanRef%")
            ?: throw IllegalArgumentException("Agreement reference: $cleanRef not found!")
        if (agreement.agreementStatus === AgreementStatus.DELETED) {
            throw BadRequestException("Avtale med anbudsnummer ${agreement.reference} er slettet, må den opprettes?")
        }
        return convertAndCreateCatalogImportResult(importedExcelCatalog.map {
            it.toCatalogImport(
                agreement,
                supplierId
            )
        }, forceUpdate)
    }

    private fun verifyCatalogImportList(catalogImportList: List<CatalogImportExcelDTO>) {
        if (catalogImportList.isEmpty()) {
            throw IllegalArgumentException("Catalog import list is empty")
        }
        if (catalogImportList.map { it.bestillingsNr }.distinct().size > 1) {
            throw IllegalArgumentException("Ugyldig katalog, inneholder flere bestillingsnr")
        }
        if (catalogImportList.map { it.supplierName }.distinct().size > 1) {
            throw IllegalArgumentException("Ugyldig katalog, inneholder flere leverandører")
        }
        if (catalogImportList.map { it.reference }.distinct().size > 1) {
            throw IllegalArgumentException("Ugylding katalog, inneholder flere rammeavtale referansenr")
        }
    }
}

data class CatalogImportResult(
    val updatedList: List<CatalogImport>,
    val deactivatedList: List<CatalogImport>,
    val insertedList: List<CatalogImport>
)