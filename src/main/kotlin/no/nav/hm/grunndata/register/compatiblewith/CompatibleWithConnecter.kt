package no.nav.hm.grunndata.register.compatiblewith

import no.nav.hm.grunndata.register.logging.sanitizeForLog
import jakarta.inject.Singleton
import kotlinx.coroutines.delay
import no.nav.hm.grunndata.rapid.dto.CatalogFileStatus
import no.nav.hm.grunndata.rapid.dto.CompatibleWith
import no.nav.hm.grunndata.rapid.dto.ServiceFor
import no.nav.hm.grunndata.register.catalog.CatalogFileRepository
import no.nav.hm.grunndata.register.catalog.CatalogImportRepository
import no.nav.hm.grunndata.register.part.CompatibleWithDTO
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.servicejob.ServiceJob
import no.nav.hm.grunndata.register.servicejob.ServiceJobRepository
import no.nav.hm.grunndata.register.servicejob.ServiceJobService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Singleton
open class CompatibleWithConnecter(
    private val compatibleAIFinder: CompatibleAIFinder,
    private val productRegistrationService: ProductRegistrationService,
    private val serviceJobRepository: ServiceJobRepository,
    private val catalogFileRepository: CatalogFileRepository,
    private val catalogImportRepository: CatalogImportRepository,
    private val serviceJobService: ServiceJobService,
    private val productRegistrationRepository: ProductRegistrationRepository
) {


    open suspend fun connectWithHmsNr(hmsNr: String, reconnect: Boolean) {
        productRegistrationService.findByExactHmsArtNr(hmsNr)?.let { product ->
            addCompatibleWithAttribute(product, reconnect).let { updatedProduct ->
                if(updatedProduct != null) {
                    productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(updatedProduct, isUpdate = true)
                }
            }
        } ?: LOG.info("No product found for hmsNr: {}", hmsNr.sanitizeForLog())
    }


    open suspend fun connectWithOrderRef(orderRef: String, reconnect: Boolean=false) {
        val productSeriesInfo =  catalogImportRepository.findCatalogProductSeriesInfoByOrderRef(orderRef)
        val parts = productSeriesInfo.filter { !it.mainProduct && it.productId != null }
        LOG.info("Found {} parts for orderRef: {} to connect and reconnect: {}", parts.size, orderRef.sanitizeForLog(), reconnect)
        parts.forEach {
            productRegistrationService.findById(it.productId!!)?.let { product ->
                addCompatibleWithAttribute(product, reconnect).let { updatedProduct ->
                    if (updatedProduct != null) {
                        productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                            updatedProduct,
                            isUpdate = true
                        )
                    }
                }
            }
            delay(500)
        }
        val serviceJobs = catalogImportRepository.findCatalogServiceJobInfoByOrderRef(orderRef)
        LOG.info("Found {} service jobs for orderRef: {} to connect", serviceJobs.size, orderRef.sanitizeForLog())
        serviceJobs.forEach {
            serviceJobRepository.findById(it.serviceId)?.let { serviceJob ->
                addServiceForAttribute(serviceJob).let { updatedService ->
                    if (updatedService != null ) {
                        serviceJobService.saveAndCreateEventIfNotDraft(updatedService, isUpdate = true)
                    }
                }
            }
            delay(500)
        }

    }

    open suspend fun connectCatalogOrderRef(orderRef: String, reconnect: Boolean)   {
        val catalogList = catalogFileRepository.findByOrderRef(orderRef)
        connectWithOrderRef(orderRef, reconnect)
        catalogList.forEach { catalogFileRepository.updatedConnectedUpdatedById(it.id, connected = true, updated = LocalDateTime.now()) }
    }

    open suspend fun connectAllOrdersNotConnected() {
        val catalogList = catalogFileRepository.findByConnectedAndStatus(connected = false, status = CatalogFileStatus.DONE)
        val orderRefGroup = catalogList.groupBy { it.orderRef }
        catalogList.distinctBy { it.orderRef }
            .forEach { catalogFile ->
                LOG.info("Connecting catalog file with orderRef: {} with name: {}", catalogFile.orderRef.sanitizeForLog(), catalogFile.fileName.sanitizeForLog())
                connectWithOrderRef(catalogFile.orderRef)
                orderRefGroup[catalogFile.orderRef]?.forEach { toUpdate ->
                    catalogFileRepository.updatedConnectedUpdatedById(toUpdate.id, connected = true, updated = LocalDateTime.now())
                }
            }
    }

    open suspend fun connectAllProductsNotConnected() {
        val products = productRegistrationService.findAccessoryOrSparePartButNoCompatibleWith()
        LOG.info("Connecting {} products", products.size)
        products.forEach { product ->
            addCompatibleWithAttribute(product).let { updatedProduct ->
                if (updatedProduct != null) {
                    productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(updatedProduct, isUpdate = true)
                }
            }
        }
    }

    open suspend fun findServiceForWithAi(hmsNr: String): List<ServiceForResult> {
        val serviceFor = catalogImportRepository.findOneByHmsArtNrOrderByCreatedDesc(hmsNr)
        if (serviceFor == null) {
            LOG.info("No catalog info found for service with hmsArtNr {}, skip connecting with serviceFor", hmsNr.sanitizeForLog())
            return emptyList()
        }
        val mainProducts = catalogImportRepository.findCatalogProductSeriesInfoByOrderRefAndMain(serviceFor.orderRef, mainProduct = true)
        if (mainProducts.isEmpty()) {
            LOG.info("No main products found for orderRef {}, skip connecting with serviceFor", serviceFor.orderRef.sanitizeForLog())
            return emptyList()
        }
        val hmsNrTitlePairs = mainProducts.map { product -> HmsNrTitlePair(product.hmsArtNr ,  product.seriesTitle)}.distinct()
        val serviceForSeries = compatibleAIFinder.findServiceableProducts(serviceFor.title, hmsNrTitlePairs)
        if (serviceForSeries.isEmpty()) {
            LOG.info("No serviceFor products found for service {}, skip connecting with serviceFor", serviceFor.hmsArtNr.sanitizeForLog())
            return emptyList()
        }

        return serviceForSeries.mapNotNull { series ->
            mainProducts.find { it.hmsArtNr == series.hmsnr }?.let { mainProduct ->
                ServiceForResult(
                    title = mainProduct.title,
                    seriesId = mainProduct.seriesId,
                    productId = mainProduct.productId,
                    hmsArtNr = mainProduct.hmsArtNr
                )
            }
        }
    }

    open suspend fun findCompatibleWithAi(hmsNr: String): List<CompatibleProductResult> {
        // find latest catalog series info for hmsNr
        val catProduct = catalogImportRepository.findOneByHmsArtNrOrderByCreatedDesc(hmsNr)
        if (catProduct == null) {
            LOG.info("No catalog product found for hmsArtNr {}, skip connecting with compatibleWith", hmsNr.sanitizeForLog())
            return emptyList()
        }
        val mainProducts = catalogImportRepository.findCatalogProductSeriesInfoByOrderRefAndMain(catProduct.orderRef, mainProduct = true)
        if (!mainProducts.isEmpty()) {
            val hmsNrTitlePairs = mainProducts.map { product -> HmsNrTitlePair(product.hmsArtNr ,  product.seriesTitle)}.distinct()
            val compatibleWithHmsnrs = compatibleAIFinder.findCompatibleProducts(catProduct.title, hmsNrTitlePairs)
            if (compatibleWithHmsnrs.isEmpty()) {
                LOG.info("No compatible series found for product {}, skip connecting with compatibleWith", catProduct.hmsArtNr.sanitizeForLog())
                return emptyList()
            }
            return compatibleWithHmsnrs.mapNotNull { compatibleHmsnr ->
                mainProducts.find { it.hmsArtNr == compatibleHmsnr.hmsnr }?.let { mainProduct ->
                    CompatibleProductResult(
                        title = mainProduct.title,
                        seriesId = mainProduct.seriesId,
                        productId = mainProduct.productId,
                        hmsArtNr = mainProduct.hmsArtNr
                    )
                }
            }
        }
        else
        {
            LOG.info("No main products found in catalog for hmsnr: {} and orderRef {}, try searching internet", hmsNr.sanitizeForLog(), catProduct.orderRef.sanitizeForLog())
            val compatibleWithHmsNrs = compatibleAIFinder.findCompatibleWhenNoMainProducts(catProduct.title, catProduct.supplierName)
            if (compatibleWithHmsNrs.isEmpty()) {
                LOG.info("No compatible series found for product {}, skip connecting with compatibleWith", catProduct.hmsArtNr.sanitizeForLog())
                return emptyList()
            }
            return compatibleWithHmsNrs.mapNotNull {
                productRegistrationRepository.findByHmsArtNrAndSupplierId(it.hmsnr, catProduct.supplierId)?.let {
                    mainProduct -> CompatibleProductResult(
                        title = mainProduct.articleName,
                        seriesId = mainProduct.seriesUUID,
                        productId = mainProduct.id,
                        hmsArtNr = mainProduct.hmsArtNr!!
                    )
                }
            }
        }

    }

    private suspend fun addServiceForAttribute(serviceJob: ServiceJob): ServiceJob? {
        // keep all previous connections.
        val productIds = serviceJob.attributes.serviceFor?.productIds?: emptySet()
        val seriesId = serviceJob.attributes.serviceFor?.seriesIds?: emptySet()
        if (serviceJob.attributes.serviceFor != null) {
            LOG.debug("Skip connecting service job {} already connected with serviceFor", serviceJob.hmsArtNr.sanitizeForLog())
            return null
        }
        if (productIds.size>99 || seriesId.size > 50) {
            LOG.error("Too many serviceFor connections for serviceJob hmsnr: {}, skip connecting with serviceFor", serviceJob.hmsArtNr.sanitizeForLog())
            return null
        }
        val serviceFors = findServiceForWithAi(serviceJob.hmsArtNr)
        if (serviceFors.isEmpty()) {
            LOG.info("No serviceFor found for serviceJob {}, skip connecting with serviceFor", serviceJob.hmsArtNr.sanitizeForLog())
            return null
        }
        val seriesIds = serviceFors.map { it.seriesId }.toSet()
        return if (seriesIds.isNotEmpty()) {
            serviceJob.copy(
                attributes = serviceJob.attributes.copy(
                    serviceFor = ServiceFor(
                        seriesIds = seriesId + seriesIds,
                        productIds = productIds
                    )
                )
            )
        } else {
            null
        }
    }

    private suspend fun addCompatibleWithAttribute(product: ProductRegistration, reconnect: Boolean = false): ProductRegistration? {
        if (!product.accessory && !product.sparePart) {
            LOG.warn("Skip connecting product {} is not accessory or sparePart", product.hmsArtNr.sanitizeForLog())
            return null
        }

        if (product.hmsArtNr == null) {
            LOG.error("No hmsArtNr for product {}, skip connecting with compatibleWith", product.id)
            return null
        }

        val seriesIdToKeep = if (reconnect) emptySet() else product.productData.attributes.compatibleWith?.seriesIds ?: emptySet()
        val productIds = if (reconnect) emptySet() else product.productData.attributes.compatibleWith?.productIds ?: emptySet()
        if (productIds.size>99 || seriesIdToKeep.size > 50) {
            LOG.error("Too many compatibleWith connections for product hmsnr: {}, skip connecting with compatibleWith", product.hmsArtNr.sanitizeForLog())
            return null
        }
        val compatibleWiths = findCompatibleWithAi(product.hmsArtNr)
        if (compatibleWiths.isEmpty()) {
            LOG.info("No compatibleWith found for product {}, skip connecting with compatibleWith", product.hmsArtNr.sanitizeForLog())
            return null
        }
        if (compatibleWiths.size > 50) {
            LOG.warn("Too many compatiblewith connections for product {}, skip connecting with compatibleWith", product.id)
            return null
        }
        val seriesIds = compatibleWiths.map { it.seriesId }.toSet()

        return if (seriesIds.isNotEmpty()) {
            product.copy(
                productData = product.productData.copy(
                    attributes = product.productData.attributes.copy(
                        compatibleWith = CompatibleWith(
                            seriesIds = seriesIdToKeep + seriesIds,
                            productIds = productIds,
                            connectedBy = CompatibleWith.COMPATIAI
                        )
                    )
                )
            )
        } else {
            null
        }
    }

    open suspend fun connectWith(compatibleWithDTO: CompatibleWithDTO, product: ProductRegistration): ProductRegistration {
        val connected = product.copy(
            productData = product.productData.copy(
                attributes = product.productData.attributes.copy(
                    compatibleWith = CompatibleWith(
                        seriesIds = compatibleWithDTO.seriesIds,
                        productIds = compatibleWithDTO.productIds,
                        connectedBy = CompatibleWith.MANUAL
                    )
                )
            )
        )
        return productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(connected, isUpdate = true)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CompatibleWithConnecter::class.java)
    }

}