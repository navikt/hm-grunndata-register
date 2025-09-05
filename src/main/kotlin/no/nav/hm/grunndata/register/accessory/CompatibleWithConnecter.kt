package no.nav.hm.grunndata.register.accessory

import jakarta.inject.Singleton
import kotlinx.coroutines.delay
import no.nav.helse.rapids_rivers.toUUID
import no.nav.hm.grunndata.rapid.dto.CatalogFileStatus
import no.nav.hm.grunndata.rapid.dto.CompatibleWith
import no.nav.hm.grunndata.register.catalog.CatalogFileRepository
import no.nav.hm.grunndata.register.catalog.CatalogImportRepository
import no.nav.hm.grunndata.register.part.CompatibleWithDTO
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Singleton
open class CompatibleWithConnecter(private val compatibleAIFinder: CompatibleAIFinder,
                                   private val productRegistrationService: ProductRegistrationService,
                                   private val catalogFileRepository: CatalogFileRepository,
                                   private val catalogImportRepository: CatalogImportRepository) {


    open suspend fun connectWithHmsNr(hmsNr: String) {
        productRegistrationService.findByExactHmsArtNr(hmsNr)?.let { product ->
            addCompatibleWithAttributeSeriesLink(product).let { updatedProduct ->
                if(updatedProduct != null) {
                    productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(updatedProduct, isUpdate = true)
                }
            }
        } ?: LOG.info("No product found for hmsNr: $hmsNr")
    }

    open suspend fun connectWithOrderRef(orderRef: String) {
        val orderRefs =  catalogImportRepository.findCatalogSeriesInfoByOrderRef(orderRef)
        val parts = orderRefs.filter { !it.mainProduct && it.productId != null }
        LOG.info("Found ${parts.size} parts for orderRef: $orderRef to connect")
        parts.forEach {
            productRegistrationService.findById(it.productId!!)?.let { product ->
                addCompatibleWithAttributeSeriesLink(product).let { updatedProduct ->
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
            addCompatibleWithAttributeSeriesLink(product).let { updatedProduct ->
                if (updatedProduct != null) {
                    productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(updatedProduct, isUpdate = true)
                }
            }
        }
    }

    open suspend fun findCompatibleWithAi(hmsNr: String): List<CompatibleProductResult> {
        val catalogSeriesInfo = catalogImportRepository.findCatalogSeriesInfosByHmsArtNrOrderByCreatedDesc(hmsNr)
        if (catalogSeriesInfo.isEmpty()) {
            LOG.info("No catalog series info found for product ${hmsNr}, skip connecting with compatibleWith")
            return emptyList()
        }
        val catProduct = catalogSeriesInfo.first()
        val mainProducts = catalogImportRepository.findCatalogImportsByOrderRefAndMain(catProduct.orderRef, mainProduct = true)

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

    private suspend fun addCompatibleWithAttributeSeriesLink(product: ProductRegistration): ProductRegistration? {
        if (product.productData.attributes.compatibleWith!= null
            && (product.productData.attributes.compatibleWith!!.connectedBy == CompatibleWith.MANUAL
                    || product.productData.attributes.compatibleWith!!.seriesIds.isNotEmpty())) {
            LOG.debug("Skip connecting product ${product.hmsArtNr} already connected with compatibleWith by ${product.productData.attributes.compatibleWith?.connectedBy}")
            return null
        }
        if (product.hmsArtNr == null) {
            LOG.error("No hmsArtNr for product ${product.id}, skip connecting with compatibleWith")
            return null
        }
        val compatibleWiths = findCompatibleWithAi(product.hmsArtNr)
        if (compatibleWiths.isEmpty()) {
            LOG.info("No compatibleWith found for product ${product.hmsArtNr}, skip connecting with compatibleWith")
            return null
        }
        val seriesIds = compatibleWiths.map { it.seriesId.toUUID() }.toSet()
        // keep all previous connections.
        val seriesIdToKeep = product.productData.attributes.compatibleWith?.seriesIds?: emptySet()
        val productIds = product.productData.attributes.compatibleWith?.productIds ?: emptySet()
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