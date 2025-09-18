package no.nav.hm.grunndata.register.product.workswith

import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.WorksWith
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationService

@Singleton
class WorksWithConnector(private val productRegistrationService: ProductRegistrationService) {


    suspend fun addConnection(worksWithMapping: WorksWithMapping): ProductRegistration {
        val sourceProduct = productRegistrationService.findById(worksWithMapping.sourceProductId) ?:
            throw IllegalArgumentException("Source Product ${worksWithMapping.sourceProductId} not found")
        if (!sourceProduct.mainProduct) throw IllegalArgumentException("Source Product ${worksWithMapping.sourceProductId} should be a main product")
        val targetProduct = productRegistrationService.findById(worksWithMapping.targetProductId) ?:
            throw IllegalArgumentException("Target Product ${worksWithMapping.targetProductId} not found")
        if (!targetProduct.mainProduct) throw IllegalArgumentException("Target Product ${worksWithMapping.targetProductId} should be a main product")
        val connected = saveWorksWithConnection(sourceProduct, targetProduct)
        // save reverse connection if not already connected
        if (targetProduct.productData.attributes.worksWith?.productIds?.contains(sourceProduct.id) != true) {
            saveWorksWithConnection(targetProduct, sourceProduct)
        }
        return connected
    }

    private suspend fun saveWorksWithConnection(
        sourceProduct: ProductRegistration,
        targetProduct: ProductRegistration
    ): ProductRegistration {
        LOG.info("Connect worksWith product ${sourceProduct.id} with ${targetProduct.id}")
        val targetSeriesIds = sourceProduct.productData.attributes.worksWith?.seriesIds ?: emptySet()
        val targetProductIds = sourceProduct.productData.attributes.worksWith?.productIds ?: emptySet()
        val connected = sourceProduct.copy(
            productData = sourceProduct.productData.copy(
                attributes = sourceProduct.productData.attributes.copy(
                    worksWith = WorksWith(
                        seriesIds = targetSeriesIds + targetProduct.seriesUUID,
                        productIds = targetProductIds + targetProduct.id
                    )
                )
            )
        )
        return productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(connected, isUpdate = true)
    }

    suspend fun removeConnection(worksWithMapping: WorksWithMapping): ProductRegistration {
        val sourceProduct = productRegistrationService.findById(worksWithMapping.sourceProductId) ?:
            throw IllegalArgumentException("Source Product ${worksWithMapping.sourceProductId} not found")
        val targetProduct = productRegistrationService.findById(worksWithMapping.targetProductId) ?:
            throw IllegalArgumentException("Target Product ${worksWithMapping.targetProductId} not found")
        val deleted = deleteWorksWithConnection(sourceProduct, targetProduct)
        if (targetProduct.productData.attributes.worksWith?.productIds?.contains(sourceProduct.id) == true) {
            deleteWorksWithConnection(targetProduct, sourceProduct)
        }
        return deleted
    }

    private suspend fun deleteWorksWithConnection(
        sourceProduct: ProductRegistration,
        targetProduct: ProductRegistration
    ): ProductRegistration {
        LOG.info("Delete worksWithConnection product ${sourceProduct.id} with ${targetProduct.id}")
        val afterRemovedSeriesIds =
            sourceProduct.productData.attributes.worksWith?.seriesIds?.minus(targetProduct.seriesUUID) ?: emptySet()
        val afterRemovedProductIds =
            sourceProduct.productData.attributes.worksWith?.productIds?.minus(targetProduct.id) ?: emptySet()
        val removed = sourceProduct.copy(
            productData = sourceProduct.productData.copy(
                attributes = sourceProduct.productData.attributes.copy(
                    worksWith = WorksWith(
                        seriesIds = afterRemovedSeriesIds,
                        productIds = afterRemovedProductIds
                    )
                )
            )
        )
        return productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(removed, isUpdate = true)
    }

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(WorksWithConnector::class.java)
    }

}