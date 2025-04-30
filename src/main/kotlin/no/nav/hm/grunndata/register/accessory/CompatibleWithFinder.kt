package no.nav.hm.grunndata.register.accessory

import jakarta.inject.Singleton
import kotlinx.coroutines.delay
import no.nav.helse.rapids_rivers.toUUID
import no.nav.hm.grunndata.rapid.dto.CatalogFileStatus
import no.nav.hm.grunndata.rapid.dto.CompatibleWith
import no.nav.hm.grunndata.register.catalog.CatalogFileRepository
import no.nav.hm.grunndata.register.catalog.CatalogImportRepository
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import org.slf4j.LoggerFactory

@Singleton
open class CompatibleWithFinder(private val compatiClient: CompatiClient,
                           private val productRegistrationService: ProductRegistrationService,
                           private val catalogFileRepository: CatalogFileRepository,
                           private val catalogImportRepository: CatalogImportRepository) {


    open suspend fun connectWithHmsNr(hmsNr: String) {
        productRegistrationService.findByHmsArtNr(hmsNr)?.let { product ->
            addCompatibleWithAttributeSeriesLink(product).let { updatedProduct ->
                if(updatedProduct != null) {
                    productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(updatedProduct, isUpdate = true)
                }
            }
        } ?: LOG.info("No product found for hmsNr: $hmsNr")
    }

    open suspend fun connectWithOrderRef(orderRef: String) {
        catalogImportRepository.findCatalogSeriesInfoByOrderRef(orderRef).filter { !it.mainProduct && it.productId != null }.forEach {
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
            delay(5000)
        }
    }

    open suspend fun connectAllOrdersNotConnected() {
        val catalogList = catalogFileRepository.findByConnectedAndStatus(connected = false, status = CatalogFileStatus.DONE)
        val orderRefGroup = catalogList.groupBy { it.orderRef }
        catalogList.distinctBy { it.orderRef }
            .forEach { catalogFile ->
                LOG.info("Connecting catalog file with orderRef: ${catalogFile.orderRef} with name: ${catalogFile.fileName}")
                connectWithOrderRef(catalogFile.orderRef)
                orderRefGroup[catalogFile.orderRef]?.forEach { toUpdate ->
                    catalogFileRepository.updateConnectedById(toUpdate.id, connected = true)
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

    open suspend fun findCompatibleWith(hmsNr: String, variant: Boolean? = false): List<CompatibleProductResult> {
        return compatiClient.findCompatibleWith(hmsNr, variant)
    }

    open suspend fun findCompatibleWithAi(hmsNr: String): List<CompatibleProductResult> {
        return compatiClient.findCompatibleWithAi(hmsNr)
    }

    private suspend fun addCompatibleWithAttributeSeriesLink(product: ProductRegistration): ProductRegistration? {
        val compatibleWiths = findCompatibleWithAi(product.hmsArtNr!!)
        val seriesIds = compatibleWiths.map { it.seriesId.toUUID() }.toSet()
        // we keep the variants, for manual by admin and supplier
        val productIds = product.productData.attributes.compatibleWith?.productIds ?: emptySet()
        return if (seriesIds.isNotEmpty()) {
            product.copy(
                productData = product.productData.copy(
                    attributes = product.productData.attributes.copy(
                        compatibleWith = CompatibleWith(
                            seriesIds = seriesIds,
                            productIds = productIds,
                            connectedBy = CompatibleWith.COMPATI
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
        private val LOG = LoggerFactory.getLogger(CompatibleWithFinder::class.java)
    }

}