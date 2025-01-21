package no.nav.hm.grunndata.register.accessory

import jakarta.inject.Singleton
import no.nav.helse.rapids_rivers.toUUID
import no.nav.hm.grunndata.rapid.dto.CompatibleWith
import no.nav.hm.grunndata.register.catalog.CatalogImportRepository
import no.nav.hm.grunndata.register.internal.maintenance.CompatibleProductController
import no.nav.hm.grunndata.register.internal.maintenance.CompatibleProductController.Companion
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import org.slf4j.LoggerFactory

@Singleton
class CompatibleWithFinder(private val compatiClient: CompatiClient,
                           private val productRegistrationService: ProductRegistrationService,
                           private val catalogImportRepository: CatalogImportRepository) {


    suspend fun connectWithHmsNr(hmsNr: String) {
        productRegistrationService.findByHmsArtNr(hmsNr)?.let { product ->
            addCompatibleWithAttributeLink(product).let { updatedProduct ->
                productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(updatedProduct, isUpdate = true)
            }
        } ?: LOG.info("No product found for hmsNr: $hmsNr")
    }

    suspend fun connectWithOrderRef(orderRef: String) {
        catalogImportRepository.findCatalogSeriesInfoByOrderRef(orderRef).filter { !it.mainProduct && it.productId != null }.forEach {
            productRegistrationService.findById(it.productId!!)?.let { product ->
                addCompatibleWithAttributeLink(product).let { updatedProduct ->
                    productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(updatedProduct, isUpdate = true)
                }
            }
        }
    }

    private suspend fun findCompatibleWith(hmsNr: String, variant: Boolean? = false): List<CompatibleProductResult> {
        return compatiClient.findCompatibleWidth(hmsNr, variant)
    }

    private suspend fun addCompatibleWithAttributeLink(product: ProductRegistration): ProductRegistration {
        val compatibleWiths = findCompatibleWith(product.hmsArtNr!!)
        val seriesIds = compatibleWiths.map { it.seriesId.toUUID() }.toSet()
        // we keep the variants, for manual by admin and supplier
        val productIds = product.productData.attributes.compatibleWidth?.productIds ?: emptySet()
        return if (seriesIds.isNotEmpty()) {
            product.copy(
                productData = product.productData.copy(
                    attributes = product.productData.attributes.copy(
                        compatibleWidth = CompatibleWith(
                            seriesIds = seriesIds,
                            productIds = productIds
                        )
                    )
                )
            )
        } else {
            LOG.info("No compatible products found for hmsNr: ${product.hmsArtNr}")
            product
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CompatibleWithFinder::class.java)
    }
}