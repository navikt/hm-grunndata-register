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
        LOG.info("persisting catalog import result with inserted: ${catalogImportResult.insertedList.size}, updated: ${catalogImportResult.updatedList.size}, deactivated: ${catalogImportResult.deactivatedList.size}")
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
                    LOG.info("Deactivated product with id: ${product.id} for HMS ArtNr: ${catalogImport.hmsArtNr} under orderRef: ${catalogImport.orderRef}")
                } else {
                    LOG.warn("Could not find product to deactivate for HMS ArtNr: ${catalogImport.hmsArtNr} under orderRef: ${catalogImport.orderRef}")
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
            LOG.error("Product ${registration.id} hmsArtNr: ${registration.hmsArtNr} has different supplierRef: ${registration.supplierRef} than catalogImport: ${catalogImport.supplierRef} under orderRef: ${catalogImport.orderRef}")
        }
        if (registration.hmsArtNr != catalogImport.hmsArtNr) {
            changedSupplierRefOrHmsNr = true
            LOG.error("Product ${registration.id} supplierRef: ${registration.supplierRef} has different hmsArtNr: ${registration.hmsArtNr} than catalogImport: ${catalogImport.hmsArtNr} under orderRef: ${catalogImport.orderRef}")
        }
        if (changedSupplierRefOrHmsNr) {
            LOG.info("Updating product ${registration.id} for HMS ArtNr: ${registration.hmsArtNr} under orderRef: ${catalogImport.orderRef} with new supplierRef: ${catalogImport.supplierRef} and new hmsArtNr: ${catalogImport.hmsArtNr}")
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
                            supplierRef = catalogImport.supplierRef,
                            isoCategory = catalogImport.iso,
                            published = catalogImport.dateFrom.atStartOfDay(),
                            expired = catalogImport.dateTo.atStartOfDay(),
                            status = mapServiceStatus(catalogImport),
                            updated = LocalDateTime.now(),
                            updatedByUser = adminAuthentication.name,
                    ))
                } ?: run {
                    LOG.info("Creating new service for HMS ArtNr: ${catalogImport.hmsArtNr} under orderRef: ${catalogImport.orderRef}")
                    val service = serviceJobRepository.save(
                        ServiceJob(
                            id = UUID.randomUUID(),
                            title = catalogImport.title,
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
                    LOG.info("Created service with id: ${service.id} for HMS ArtNr: ${catalogImport.hmsArtNr} under orderRef: ${catalogImport.orderRef}")
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
                            expired = LocalDateTime.now(),
                            status = ServiceStatus.INACTIVE,
                            updated = LocalDateTime.now(),
                            updatedByUser = adminAuthentication.name
                        )
                    )
                    LOG.info("Deactivated service with id: ${service.id} for HMS ArtNr: ${catalogImport.hmsArtNr} under orderRef: ${catalogImport.orderRef}")
                } else {
                    LOG.warn("Could not find service to deactivate for HMS ArtNr: ${catalogImport.hmsArtNr} under orderRef: ${catalogImport.orderRef}")
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
        LOG.info("Created product with id: ${product.id} for HMS ArtNr: ${catalogImport.hmsArtNr} mainProduct: ${catalogImport.mainProduct} under orderRef: ${catalogImport.orderRef}")
        return product
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