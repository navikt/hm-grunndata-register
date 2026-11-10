package no.nav.hm.grunndata.register.catalog

import io.micronaut.security.authentication.ClientAuthentication
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.AgreementStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.rapid.dto.ServiceStatus
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationDTO
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
import no.nav.hm.grunndata.register.servicejob.ServiceJob
import no.nav.hm.grunndata.register.servicejob.ServiceJobRepository
import no.nav.hm.grunndata.register.logging.sanitizeForLog
import org.slf4j.LoggerFactory
import java.util.UUID

@Singleton
open class CatalogImportService(
    private val catalogImportRepository: CatalogImportRepository,
    private val agreementRegistrationService: AgreementRegistrationService,
    private val productRegistrationRepository: ProductRegistrationRepository,
    private val seriesRegistrationRepository: SeriesRegistrationRepository,
    private val serviceJobRepository: ServiceJobRepository
) {
    @Transactional
    open suspend fun checkForExistingAndMapCatalogImportResult(
        catalogImportList: List<CatalogImport>,
        forceUpdate: Boolean
    ): CatalogImportResult {
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
            it.copy(dateTo = LocalDate.now().minusDays(1), updated = LocalDateTime.now())
        }
        return CatalogImportResult(updatedList, deactivatedList, insertedList)
    }

    @Transactional
    open suspend fun persistCatalogImportResult(catalogImportResult: CatalogImportResult) {
        LOG.info("persisting catalog import result with inserted: {}, updated: {}, deactivated: {}", catalogImportResult.insertedList.size, catalogImportResult.updatedList.size, catalogImportResult.deactivatedList.size)
        catalogImportResult.updatedList.forEach { catalogImportRepository.update(it) }
        catalogImportResult.deactivatedList.forEach { catalogImportRepository.update(it) }
        catalogImportResult.insertedList.forEach { catalogImportRepository.save(it) }
    }

    suspend fun mapExcelDTOToCatalogImport(
        importedExcelCatalog: List<CatalogImportExcelDTO>,
        supplierId: UUID,
    ): Pair<AgreementRegistrationDTO, List<CatalogImport>> {
        verifyCatalogImportList(importedExcelCatalog)
        val agreementRef = importedExcelCatalog.first().reference
        val cleanRef = agreementRef.lowercase().replace("/", "-")
        val agreement = agreementRegistrationService.findByReferenceLike("%$cleanRef%")
            ?: throw IllegalArgumentException("Agreement reference: $cleanRef not found!")
        if (agreement.agreementStatus === AgreementStatus.DELETED) {
            throw BadRequestException("Avtale med anbudsnummer ${agreement.reference} er slettet, må den opprettes?")
        }
        return agreement to importedExcelCatalog.map { it.toCatalogImport(
                agreement,
                supplierId)}
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

    suspend fun handleNewProductsOrChangedSupplierRefFromCatalogImport(
        catalogImportResult: CatalogImportResult,
        adminAuthentication: ClientAuthentication
    ) {
        val updates =
            catalogImportResult.insertedList + catalogImportResult.updatedList + catalogImportResult.deactivatedList
        if (updates.isNotEmpty()) {
            updates.forEach { catalogImport ->
                val product = productRegistrationRepository.findByHmsArtNrAndSupplierId(
                    catalogImport.hmsArtNr,
                    catalogImport.supplierId
                )
                    ?: productRegistrationRepository.findBySupplierRefAndSupplierId(
                        catalogImport.supplierRef,
                        catalogImport.supplierId
                    )
                product?.let {
                    checkSupplierRefAndUpdate(it, catalogImport, adminAuthentication)
                } ?: createNewProductAndSeries(catalogImport, adminAuthentication)
            }
        }
        if (catalogImportResult.deactivatedList.isNotEmpty()) {
            catalogImportResult.deactivatedList.forEach { catalogImport ->
                val product = productRegistrationRepository.findByHmsArtNrAndSupplierId(
                    catalogImport.hmsArtNr,
                    catalogImport.supplierId
                )
                    ?: productRegistrationRepository.findBySupplierRefAndSupplierId(
                        catalogImport.supplierRef,
                        catalogImport.supplierId
                    )
                if (product != null && !product.mainProduct) { // we only deactivate none main products
                    productRegistrationRepository.update(
                        product.copy(
                            registrationStatus = RegistrationStatus.INACTIVE,
                            expired = LocalDateTime.now(),
                            updatedByUser = adminAuthentication.name
                        )
                    )
                    LOG.info("Deactivated product with id: {} for HMS ArtNr: {} under orderRef: {}", product.id, catalogImport.hmsArtNr.sanitizeForLog(), catalogImport.orderRef.sanitizeForLog())
                } else {
                    LOG.warn("Could not find product to deactivate for HMS ArtNr: {} under orderRef: {}", catalogImport.hmsArtNr.sanitizeForLog(), catalogImport.orderRef.sanitizeForLog())
                }
            }
        }
    }

    private suspend fun checkSupplierRefAndUpdate(
        registration: ProductRegistration,
        catalogImport: CatalogImport,
        adminAuthentication: ClientAuthentication
    ) {
        var changedSupplierRefOrHmsNr = false
        if (registration.supplierRef != catalogImport.supplierRef) {
            changedSupplierRefOrHmsNr = true
            LOG.error("Product {} hmsArtNr: {} has different supplierRef: {} than catalogImport: {} under orderRef: {}", registration.id, registration.hmsArtNr.sanitizeForLog(), registration.supplierRef.sanitizeForLog(), catalogImport.supplierRef.sanitizeForLog(), catalogImport.orderRef.sanitizeForLog())
        }
        if (registration.hmsArtNr != catalogImport.hmsArtNr) {
            changedSupplierRefOrHmsNr = true
            LOG.error("Product {} supplierRef: {} has different hmsArtNr: {} than catalogImport: {} under orderRef: {}", registration.id, registration.supplierRef.sanitizeForLog(), registration.hmsArtNr.sanitizeForLog(), catalogImport.hmsArtNr.sanitizeForLog(), catalogImport.orderRef.sanitizeForLog())
        }
        if (changedSupplierRefOrHmsNr) {
            LOG.info("Updating product {} for HMS ArtNr: {} under orderRef: {} with new supplierRef: {} and new hmsArtNr: {}", registration.id, registration.hmsArtNr.sanitizeForLog(), catalogImport.orderRef.sanitizeForLog(), catalogImport.supplierRef.sanitizeForLog(), catalogImport.hmsArtNr.sanitizeForLog())
            productRegistrationRepository.update(
                registration.copy(
                    articleName = catalogImport.title,
                    hmsArtNr = catalogImport.hmsArtNr,
                    supplierRef = catalogImport.supplierRef,
                    updatedByUser = adminAuthentication.name,
                    updated = LocalDateTime.now()
                )
            )
        }
    }


    suspend fun handleNewServices(serviceImportResult: CatalogImportResult, adminAuthentication: ClientAuthentication) {
        val updates = serviceImportResult.insertedList + serviceImportResult.updatedList
        if (updates.isNotEmpty()) {
            updates.forEach { catalogImport ->
                serviceJobRepository.findBySupplierIdAndHmsArtNr(
                    catalogImport.supplierId,
                    catalogImport.hmsArtNr
                )?.let {
                    serviceJobRepository.update(
                        it.copy(
                            title = catalogImport.title,
                            draftStatus = DraftStatus.DONE,
                            supplierRef = catalogImport.supplierRef,
                            isoCategory = catalogImport.iso,
                            published = catalogImport.dateFrom.atStartOfDay(),
                            expired = catalogImport.dateTo.atStartOfDay(),
                            status = mapServiceStatus(catalogImport),
                            updated = LocalDateTime.now(),
                            updatedByUser = adminAuthentication.name,
                    ))
                } ?: run {
                    LOG.info("Creating new service for HMS ArtNr: {} under orderRef: {}", catalogImport.hmsArtNr.sanitizeForLog(), catalogImport.orderRef.sanitizeForLog())
                    val service = serviceJobRepository.save(
                        ServiceJob(
                            id = UUID.randomUUID(),
                            title = catalogImport.title,
                            draftStatus = DraftStatus.DONE,
                            supplierRef = catalogImport.supplierRef,
                            hmsArtNr = catalogImport.hmsArtNr,
                            supplierId = catalogImport.supplierId,
                            isoCategory = catalogImport.iso,
                            published = catalogImport.dateFrom.atStartOfDay(),
                            expired = catalogImport.dateTo.atStartOfDay(),
                            status = mapServiceStatus(catalogImport),
                            createdBy = EXCEL,
                            createdByUser = adminAuthentication.name,
                            updatedByUser = adminAuthentication.name,
                        )
                    )
                    LOG.info("Created service with id: {} for HMS ArtNr: {} under orderRef: {}", service.id, catalogImport.hmsArtNr.sanitizeForLog(), catalogImport.orderRef.sanitizeForLog())
                }
            }
        }
        if (serviceImportResult.deactivatedList.isNotEmpty()) {
            serviceImportResult.deactivatedList.forEach { catalogImport ->
                val service = serviceJobRepository.findBySupplierIdAndHmsArtNr(
                    catalogImport.supplierId,  catalogImport.hmsArtNr,
                )
                if (service != null) {
                    serviceJobRepository.update(
                        service.copy(
                            draftStatus = DraftStatus.DONE,
                            expired = LocalDateTime.now(),
                            status = ServiceStatus.INACTIVE,
                            updated = LocalDateTime.now(),
                            updatedByUser = adminAuthentication.name
                        )
                    )
                    LOG.info("Deactivated service with id: {} for HMS ArtNr: {} under orderRef: {}", service.id, catalogImport.hmsArtNr.sanitizeForLog(), catalogImport.orderRef.sanitizeForLog())
                } else {
                    LOG.warn("Could not find service to deactivate for HMS ArtNr: {} under orderRef: {}", catalogImport.hmsArtNr.sanitizeForLog(), catalogImport.orderRef.sanitizeForLog())
                }
            }
        }
    }

    private fun mapServiceStatus(catalogImport: CatalogImport): ServiceStatus {
        val nowDate = LocalDate.now()
        return if (catalogImport.dateFrom <= nowDate && catalogImport.dateTo > nowDate) ServiceStatus.ACTIVE
        else ServiceStatus.INACTIVE
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
            )
        )
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
            )
        )
        LOG.info("Created product with id: {} for HMS ArtNr: {} mainProduct: {} under orderRef: {}", product.id, catalogImport.hmsArtNr.sanitizeForLog(), catalogImport.mainProduct, catalogImport.orderRef.sanitizeForLog())
        return product
    }


    suspend fun checkForProductRegistrationConflicts(catalogImportResult: CatalogImportResult): List<DuplicateConflict> {
        val conflicts = mutableListOf<DuplicateConflict>()
        val toCheck = (catalogImportResult.insertedList + catalogImportResult.updatedList).filter { it.isProduct() }
        toCheck.forEach { catalogImport ->
            val foundByHmsArtNr = productRegistrationRepository.findByHmsArtNrAndSupplierId(
                catalogImport.hmsArtNr, catalogImport.supplierId
            )
            val foundBySupplierRef = productRegistrationRepository.findBySupplierRefAndSupplierId(
                catalogImport.supplierRef, catalogImport.supplierId
            )
            if (foundByHmsArtNr != null &&
                foundByHmsArtNr.supplierRef != catalogImport.supplierRef &&
                foundBySupplierRef != null &&
                foundBySupplierRef.id != foundByHmsArtNr.id
            ) {
                conflicts.add(
                    DuplicateConflict(
                        hmsArtNr = catalogImport.hmsArtNr,
                        supplierRef = catalogImport.supplierRef,
                        supplierId = catalogImport.supplierId,
                        conflictingProductId = foundBySupplierRef.id,
                        conflictingHmsArtNr = foundBySupplierRef.hmsArtNr,
                        message = "Updating supplierRef on product ${foundByHmsArtNr.id} (hmsArtNr: ${foundByHmsArtNr.hmsArtNr}) " +
                                "would violate unique constraint: (supplierId, supplierRef=${catalogImport.supplierRef}) " +
                                "already exists on product ${foundBySupplierRef.id} (hmsArtNr: ${foundBySupplierRef.hmsArtNr})"
                    )
                )
            }
        }
        return conflicts
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CatalogImportService::class.java)
    }
}

fun CatalogImport.isProduct(): Boolean {
    return mainProduct || accessory || sparePart
}

fun CatalogImport.isService(): Boolean {
    return !isProduct()
}

data class CatalogImportResult(
    val updatedList: List<CatalogImport>,
    val deactivatedList: List<CatalogImport>,
    val insertedList: List<CatalogImport>
)

data class DuplicateConflict(
    val hmsArtNr: String,
    val supplierRef: String,
    val supplierId: UUID,
    val conflictingProductId: UUID,
    val conflictingHmsArtNr: String?,
    val message: String,
)