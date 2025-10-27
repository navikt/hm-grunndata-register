package no.nav.hm.grunndata.register.catalog

import io.micronaut.security.authentication.ClientAuthentication
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.AgreementStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationService
import no.nav.hm.grunndata.register.catalog.ProductAgreementImportExcelService.Companion.EXCEL
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.product.ProductData
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import no.nav.hm.grunndata.register.product.isAdmin
import no.nav.hm.grunndata.register.series.SeriesDataDTO
import no.nav.hm.grunndata.register.series.SeriesRegistration
import no.nav.hm.grunndata.register.series.SeriesRegistrationRepository
import java.util.UUID

@Singleton
open class CatalogImportService(
    private val catalogImportRepository: CatalogImportRepository,
    private val agreementRegistrationService: AgreementRegistrationService,
    private val productRegistrationRepository: ProductRegistrationRepository,
    private val seriesRegistrationRepository: SeriesRegistrationRepository
) {
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
                val existing = existingCatalog.find { it.hmsArtNr == catalogImport.hmsArtNr }
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

    suspend fun handleNewOrChangedSupplierRefFromCatalogImport(
        catalogImportResult: CatalogImportResult,
        adminAuthentication: ClientAuthentication) {
        val updates = catalogImportResult.insertedList + catalogImportResult.updatedList + catalogImportResult.deactivatedList
        if (updates.isNotEmpty()) {
            updates.forEach { catalogImport ->
                val product = productRegistrationRepository.findByHmsArtNrAndSupplierId(catalogImport.hmsArtNr, catalogImport.supplierId)
                    ?: productRegistrationRepository.findBySupplierRefAndSupplierId(catalogImport.supplierRef,catalogImport.supplierId)
                product?.let {
                    var changedSupplierRefOrHmsNr = false
                    if (it.supplierRef != catalogImport.supplierRef) {
                        changedSupplierRefOrHmsNr = true
                        LOG.error("Product ${it.id} hmsArtNr: ${it.hmsArtNr} has different supplierRef: ${it.supplierRef} than catalogImport: ${catalogImport.supplierRef} under orderRef: ${catalogImport.orderRef}")
                    }
                    if (it.hmsArtNr != catalogImport.hmsArtNr) {
                        changedSupplierRefOrHmsNr = true
                        LOG.error("Product ${it.id} supplierRef: ${it.supplierRef} has different hmsArtNr: ${it.hmsArtNr} than catalogImport: ${catalogImport.hmsArtNr} under orderRef: ${catalogImport.orderRef}")
                    }
                    if (changedSupplierRefOrHmsNr) {
                        LOG.info("Updating product ${it.id} for HMS ArtNr: ${it.hmsArtNr} under orderRef: ${catalogImport.orderRef} with new supplierRef: ${catalogImport.supplierRef} and new hmsArtNr: ${catalogImport.hmsArtNr}" )
                        productRegistrationRepository.update(
                            it.copy(
                                articleName = catalogImport.title,
                                hmsArtNr = catalogImport.hmsArtNr,
                                supplierRef = catalogImport.supplierRef,
                                accessory = catalogImport.accessory,
                                sparePart = catalogImport.sparePart,
                                mainProduct = catalogImport.mainProduct,
                                updatedByUser = adminAuthentication.name,
                                updated = LocalDateTime.now()
                            )
                        )
                    }
                } ?: createNewProductAndSeries(catalogImport, adminAuthentication)
            }
        }
        if (catalogImportResult.deactivatedList.isNotEmpty()) {
            catalogImportResult.deactivatedList.forEach { catalogImport ->
                val product = productRegistrationRepository.findByHmsArtNrAndSupplierId(catalogImport.hmsArtNr, catalogImport.supplierId)
                    ?: productRegistrationRepository.findBySupplierRefAndSupplierId(catalogImport.supplierRef,catalogImport.supplierId)
                if (product != null && !product.mainProduct) { // we only deactivate none main products
                    productRegistrationRepository.update(
                        product.copy(
                            registrationStatus = RegistrationStatus.INACTIVE,
                            expired = LocalDateTime.now(),
                            updatedByUser = adminAuthentication.name
                        )
                    )
                    LOG.info("Deactivated product with id: ${product.id} for HMS ArtNr: ${catalogImport.hmsArtNr} under orderRef: ${catalogImport.orderRef}" )
                } else {
                    LOG.warn("Could not find product to deactivate for HMS ArtNr: ${catalogImport.hmsArtNr} under orderRef: ${catalogImport.orderRef}" )
                }
            }
        }
    }

    private suspend fun createNewProductAndSeries(
        catalogImport: CatalogImport,
        authentication: ClientAuthentication
    ): ProductRegistration {
        val seriesId = UUID.randomUUID()
        val series = seriesRegistrationRepository.save(
            SeriesRegistration(
                id = seriesId,
                draftStatus = DraftStatus.DONE,
                adminStatus = if (catalogImport.mainProduct) AdminStatus.PENDING else AdminStatus.APPROVED,
                supplierId = catalogImport.supplierId,
                title = catalogImport.title,
                identifier = seriesId.toString(),
                isoCategory = catalogImport.iso,
                status = SeriesStatus.ACTIVE,
                seriesData = SeriesDataDTO(),
                text = catalogImport.title.trim(),
                createdByUser = authentication.name,
                updatedByUser = authentication.name,
                createdByAdmin = authentication.isAdmin(),
                createdBy = EXCEL,
                mainProduct = catalogImport.mainProduct,
            ))
        val product = productRegistrationRepository.save(
            ProductRegistration(
                seriesUUID = series.id,
                draftStatus = DraftStatus.DONE,
                adminStatus = if (catalogImport.mainProduct) AdminStatus.PENDING else AdminStatus.APPROVED,
                registrationStatus = RegistrationStatus.ACTIVE,
                articleName = catalogImport.title,
                productData = ProductData(),
                supplierId = catalogImport.supplierId,
                hmsArtNr = catalogImport.hmsArtNr,
                id = UUID.randomUUID(),
                supplierRef = catalogImport.supplierRef,
                accessory = catalogImport.accessory,
                sparePart = catalogImport.sparePart,
                mainProduct = catalogImport.mainProduct,
                createdByUser = authentication.name,
                updatedByUser = authentication.name,
                createdBy = EXCEL,
                createdByAdmin = authentication.isAdmin(),
            ))
        LOG.info("Created product with id: ${product.id} for HMS ArtNr: ${catalogImport.hmsArtNr} mainProduct: ${catalogImport.mainProduct} under orderRef: ${catalogImport.orderRef}" )
        return product
    }
    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(CatalogImportService::class.java)
    }
}

data class CatalogImportResult(
    val updatedList: List<CatalogImport>,
    val deactivatedList: List<CatalogImport>,
    val insertedList: List<CatalogImport>
)