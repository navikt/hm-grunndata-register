package no.nav.hm.grunndata.register.accessory

import jakarta.inject.Singleton
import no.nav.helse.rapids_rivers.toUUID
import no.nav.hm.grunndata.rapid.dto.CompatibleWith
import no.nav.hm.grunndata.register.product.ProductRegistration
import org.slf4j.LoggerFactory

@Singleton
class CompatibleWithFinder(private val compatiClient: CompatiClient) {

    suspend fun findCompatibleWith(hmsNr: String, variant: Boolean? = false): List<CompatibleProductResult> {
        return compatiClient.findCompatibleWidth(hmsNr, variant)
    }

    suspend fun addCompatibleWithAttributeLink(product: ProductRegistration): ProductRegistration {
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