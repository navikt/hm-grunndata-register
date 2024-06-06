package no.nav.hm.grunndata.register.series

import jakarta.inject.Singleton
import java.time.LocalDateTime
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import org.slf4j.LoggerFactory


@Singleton
class SeriesStateHandler(private val seriesRegistrationRepository: SeriesRegistrationRepository,
                         private val productRegistrationRepository: ProductRegistrationRepository
) {
    // REMEMBER TO remove this when HMD is shut down
    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesStateHandler::class.java)
    }

    suspend fun findProductsThatHasNoSeries() {
        val productsWithNoSeries = productRegistrationRepository.findProductsWithNoSeries()
        LOG.info("Found ${productsWithNoSeries.size} products with no series")
        productsWithNoSeries.forEach {
            seriesRegistrationRepository.save(
                SeriesRegistration(
                    id = it.seriesUUID,
                    supplierId = it.supplierId,
                    title = it.title,
                    text = it.productData.attributes.text?:"",
                    draftStatus = DraftStatus.DONE,
                    isoCategory = it.isoCategory,
                    identifier = it.productData.seriesIdentifier?: UUID.randomUUID().toString(),
                    status = SeriesStatus.ACTIVE,
                    seriesData = SeriesDataDTO(media = it.productData.media)
                )
            )
        }
        LOG.info("finished creating series for ${productsWithNoSeries.size} products")
    }

    suspend fun copyMediaFromProductsToSeries() {
        var count = 0
        seriesRegistrationRepository.findAll().collect {
            count++
            val product = productRegistrationRepository.findBySeriesUUID(it.id)
            if (product != null) {
                val media = product.productData.media
                if (media.isNotEmpty()) {
                    LOG.info("Updating series ${it.id} with media from product ${product.id}")
                    val seriesData = it.seriesData.copy(media = media)
                    seriesRegistrationRepository.update(it.copy(seriesData = seriesData))
                }
            }
        }
        LOG.info("Finished copying media from products to series for $count series")
    }

    suspend fun findEmptyAndDeleteSeries() {
        val emptySeries = seriesRegistrationRepository.findSeriesThatDoesNotHaveProducts()
        LOG.info("Found empty ${emptySeries.size} series")
        emptySeries.filter { it.status != SeriesStatus.DELETED }.forEach {
             LOG.info("set series ${it.id} to DELETED")
            seriesRegistrationRepository.update(it.copy(status = SeriesStatus.DELETED))
        }
    }

    suspend fun findEmptyCategorySeriesAndPopulateWithProductsData() {
        val emptyCategorySeries = seriesRegistrationRepository.findByIsoCategory("")
        LOG.info("Found empty iso cateories ${emptyCategorySeries.size} series")
        emptyCategorySeries.forEach { series ->
            productRegistrationRepository.findBySeriesUUID(series.id)?.let { product ->
                seriesRegistrationRepository.update(series.copy(isoCategory = product.isoCategory, title = product.title,
                    updated = LocalDateTime.now(), seriesData = SeriesDataDTO(media = product.productData.media), text = product.productData.attributes.text?:""
                ))
            }
        }
    }

}