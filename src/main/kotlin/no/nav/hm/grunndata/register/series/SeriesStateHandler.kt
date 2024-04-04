package no.nav.hm.grunndata.register.series

import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import org.slf4j.LoggerFactory

@Singleton
class SeriesStateHandler(private val seriesRegistrationRepository: SeriesRegistrationRepository,
                         private val productRegistrationRepository: ProductRegistrationRepository
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesStateHandler::class.java)
    }

    suspend fun syncronizeProductWithSeries() {
        val productsWithNoSeries = productRegistrationRepository.findBySeriesIdNotExists()
        LOG.info("Found ${productsWithNoSeries.size} products with no series")
        // disabled for now
//        productsWithNoSeries.forEach {
//            seriesRegistrationRepository.save(
//                SeriesRegistration(
//                    id = it.seriesUUID,
//                    supplierId = it.supplierId,
//                    title = it.title,
//                    text = it.productData.attributes.text?:"",
//                    draftStatus = DraftStatus.DONE,
//                    isoCategory = it.isoCategory,
//                    identifier = it.productData.seriesIdentifier?: UUID.randomUUID().toString(),
//                    status = SeriesStatus.ACTIVE
//                )
//            )
//        }
    }
}