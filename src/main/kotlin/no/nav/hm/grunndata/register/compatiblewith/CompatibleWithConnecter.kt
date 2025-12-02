package no.nav.hm.grunndata.register.compatiblewith

import jakarta.inject.Singleton
import kotlinx.coroutines.delay
import no.nav.helse.rapids_rivers.toUUID
import no.nav.hm.grunndata.rapid.dto.CatalogFileStatus
import no.nav.hm.grunndata.rapid.dto.CompatibleWith
import no.nav.hm.grunndata.rapid.dto.ServiceFor
import no.nav.hm.grunndata.register.catalog.CatalogFileRepository
import no.nav.hm.grunndata.register.catalog.CatalogImportRepository
import no.nav.hm.grunndata.register.part.CompatibleWithDTO
import no.nav.hm.grunndata.register.product.ProductRegistration
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
    private val serviceJobService: ServiceJobService
) {


    open suspend fun connectWithHmsNr(hmsNr: String) {
        productRegistrationService.findByExactHmsArtNr(hmsNr)?.let { product ->
            addCompatibleWithAttribute(product).let { updatedProduct ->
                if(updatedProduct != null) {
                    productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(updatedProduct, isUpdate = true)
                }
            }
        } ?: LOG.info("No product found for hmsNr: $hmsNr")
    }


    open suspend fun connectWithOrderRef(orderRef: String) {
        val productSeriesInfo =  catalogImportRepository.findCatalogProductSeriesInfoByOrderRef(orderRef)
        val parts = productSeriesInfo.filter { !it.mainProduct && it.productId != null }
        LOG.info("Found ${parts.size} parts for orderRef: $orderRef to connect")
        parts.forEach {
            productRegistrationService.findById(it.productId!!)?.let { product ->
                addCompatibleWithAttribute(product).let { updatedProduct ->
                    if (updatedProduct != null) {
                        productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                            updatedProduct,
                            isUpdate = true
                        )
                    }
                }
            }
            delay(1000)
        }
        val serviceJobs = catalogImportRepository.findCatalogServiceJobInfoByOrderRef(orderRef)
        LOG.info("Found ${serviceJobs.size} service jobs for orderRef: $orderRef to connect")
        serviceJobs.forEach {
            serviceJobRepository.findById(it.serviceId)?.let { serviceJob ->
                addServiceForAttribute(serviceJob).let { updatedService ->
                    if (updatedService != null ) {
                        serviceJobService.saveAndCreateEventIfNotDraft(updatedService, isUpdate = true)
                    }
                }
            }
            delay(1000)
        }

    }

    open suspend fun connectCatalogOrderRef(orderRef: String)   {
        val catalogList = catalogFileRepository.findByOrderRef(orderRef)
        connectWithOrderRef(orderRef)
        catalogList.forEach { catalogFileRepository.updatedConnectedUpdatedById(it.id, connected = true, updated = LocalDateTime.now()) }
    }

    open suspend fun connectAllOrdersNotConnected() {
        val catalogList = catalogFileRepository.findByConnectedAndStatus(connected = false, status = CatalogFileStatus.DONE)
        val orderRefGroup = catalogList.groupBy { it.orderRef }
        catalogList.distinctBy { it.orderRef }
            .forEach { catalogFile ->
                LOG.info("Connecting catalog file with orderRef: ${catalogFile.orderRef} with name: ${catalogFile.fileName}")
                connectWithOrderRef(catalogFile.orderRef)
                orderRefGroup[catalogFile.orderRef]?.forEach { toUpdate ->
                    catalogFileRepository.updatedConnectedUpdatedById(toUpdate.id, connected = true, updated = LocalDateTime.now())
                }
            }
    }

    open suspend fun connectAllProductsNotConnected() {
        val products = productRegistrationService.findAccessoryOrSparePartButNoCompatibleWith()
        LOG.info("Connecting ${products.size} products")
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
            LOG.info("No catalog info found for service with hmsArtNr $hmsNr, skip connecting with serviceFor")
            return emptyList()
        }
        val mainProducts = catalogImportRepository.findCatalogProductSeriesInfoByOrderRefAndMain(serviceFor.orderRef, mainProduct = true)
        if (mainProducts.isEmpty()) {
            LOG.info("No main products found for orderRef ${serviceFor.orderRef}, skip connecting with serviceFor")
            return emptyList()
        }
        val hmsNrTitlePairs = mainProducts.map { product -> HmsNrTitlePair(product.hmsArtNr ,  product.seriesTitle)}.distinct()
        val serviceForSeries = compatibleAIFinder.findServiceableProducts(serviceFor.title, hmsNrTitlePairs)
        if (serviceForSeries.isEmpty()) {
            LOG.info("No serviceFor products found for service ${serviceFor.hmsArtNr}, skip connecting with serviceFor")
            return emptyList()
        }

        return serviceForSeries.mapNotNull { series ->
            mainProducts.find { it.hmsArtNr == series.hmsnr }?.let { mainProduct ->
                ServiceForResult(
                    title = mainProduct.title,
                    seriesId = mainProduct.seriesId.toString(),
                    productId = mainProduct.productId?.toString() ?: "",
                    hmsArtNr = mainProduct.hmsArtNr
                )
            }
        }
    }

    open suspend fun findCompatibleWithAi(hmsNr: String): List<CompatibleProductResult> {
        // find latest catalog series info for hmsNr
        val catProduct = catalogImportRepository.findOneByHmsArtNrOrderByCreatedDesc(hmsNr)
        if (catProduct == null) {
            LOG.info("No catalog product found for hmsArtNr $hmsNr, skip connecting with compatibleWith")
            return emptyList()
        }
        val mainProducts = catalogImportRepository.findCatalogProductSeriesInfoByOrderRefAndMain(catProduct.orderRef, mainProduct = true)
        if (mainProducts.isEmpty()) {
            LOG.info("No main products found for orderRef ${catProduct.orderRef}, skip connecting with compatibleWith")
            return emptyList()
        }
        val hmsNrTitlePairs = mainProducts.map { product -> HmsNrTitlePair(product.hmsArtNr ,  product.seriesTitle)}.distinct()
        val compatibleWithSeries = compatibleAIFinder.findCompatibleProducts(catProduct.title, hmsNrTitlePairs)
        if (compatibleWithSeries.isEmpty()) {
            LOG.info("No compatible series found for product ${catProduct.hmsArtNr}, skip connecting with compatibleWith")
            return emptyList()
        }

        return compatibleWithSeries.mapNotNull { series ->
            mainProducts.find { it.hmsArtNr == series.hmsnr }?.let { mainProduct ->
                CompatibleProductResult(
                    title = mainProduct.title,
                    seriesTitle = mainProduct.seriesTitle,
                    seriesId = mainProduct.seriesId.toString(),
                    productId = mainProduct.productId?.toString() ?: "",
                    hmsArtNr = mainProduct.hmsArtNr
                )
            }
        }
    }

    private suspend fun addServiceForAttribute(serviceJob: ServiceJob): ServiceJob? {
        // keep all previous connections.
        val productIds = serviceJob.attributes.serviceFor?.productIds?: emptySet()
        val seriesId = serviceJob.attributes.serviceFor?.seriesIds?: emptySet()
        if (productIds.size>99 || seriesId.size > 50) {
            LOG.error("Too many serviceFor connections for serviceJob hmsnr: ${serviceJob.hmsArtNr}, skip connecting with serviceFor")
            return null
        }
        val serviceFors = findServiceForWithAi(serviceJob.hmsArtNr)
        if (serviceFors.isEmpty()) {
            LOG.info("No serviceFor found for serviceJob ${serviceJob.hmsArtNr}, skip connecting with serviceFor")
            return null
        }
        val seriesIds = serviceFors.map { it.seriesId.toUUID() }.toSet()
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

    private suspend fun addCompatibleWithAttribute(product: ProductRegistration): ProductRegistration? {
        if (!product.accessory && !product.sparePart) {
            LOG.warn("Skip connecting product ${product.hmsArtNr} is not accessory or sparePart")
            return null
        }
        if (product.productData.attributes.compatibleWith!= null
            && product.productData.attributes.compatibleWith!!.connectedBy == CompatibleWith.MANUAL) {
            LOG.debug("Skip connecting product ${product.hmsArtNr} already connected with compatibleWith by ${product.productData.attributes.compatibleWith?.connectedBy}")
            return null
        }
        if (product.hmsArtNr == null) {
            LOG.error("No hmsArtNr for product ${product.id}, skip connecting with compatibleWith")
            return null
        }
        // keep all previous connections.
        val seriesIdToKeep = product.productData.attributes.compatibleWith?.seriesIds?: emptySet()
        val productIds = product.productData.attributes.compatibleWith?.productIds ?: emptySet()
        if (productIds.size>99 || seriesIdToKeep.size > 50) {
            LOG.error("Too many compatibleWith connections for product hmsnr: ${product.hmsArtNr}, skip connecting with compatibleWith")
            return null
        }
        val compatibleWiths = findCompatibleWithAi(product.hmsArtNr)
        if (compatibleWiths.isEmpty()) {
            LOG.info("No compatibleWith found for product ${product.hmsArtNr}, skip connecting with compatibleWith")
            return null
        }
        val seriesIds = compatibleWiths.map { it.seriesId.toUUID() }.toSet()

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