package no.nav.hm.grunndata.register.series

import io.micronaut.context.annotation.Factory
import io.micronaut.data.event.listeners.PostPersistEventListener
import io.micronaut.data.event.listeners.PostUpdateEventListener
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.product.ProductRegistration
import org.slf4j.LoggerFactory

@Factory
class ProductPersistListener(private val seriesRegistrationRepository: SeriesRegistrationRepository) {
    companion object {
        private val LOG = LoggerFactory.getLogger(ProductPersistListener::class.java)
    }

    @Singleton
    fun afterProductPersist(): PostPersistEventListener<ProductRegistration> {
        return PostPersistEventListener { product: ProductRegistration ->
            runBlocking {
                LOG.debug("ProductRegistration inserted for series: ${product.seriesUUID}")
                seriesRegistrationRepository.updateCountForSeries(product.seriesUUID)
                seriesRegistrationRepository.resetCountStatusesForSeries(product.seriesUUID)
                seriesRegistrationRepository.updateCountDraftsForSeries(product.seriesUUID)
                seriesRegistrationRepository.updateCountPublishedForSeries(product.seriesUUID)
                seriesRegistrationRepository.updateCountPendingForSeries(product.seriesUUID)
                seriesRegistrationRepository.updateCountDeclinedForSeries(product.seriesUUID)
            }
        }
    }

    @Singleton
    fun afterProductUpdate(): PostUpdateEventListener<ProductRegistration> {
        return PostUpdateEventListener { product: ProductRegistration ->
            runBlocking {
                LOG.debug("ProductRegistration updated for series: ${product.seriesUUID}")
                seriesRegistrationRepository.updateCountForSeries(product.seriesUUID)
                seriesRegistrationRepository.resetCountStatusesForSeries(product.seriesUUID)
                seriesRegistrationRepository.updateCountDraftsForSeries(product.seriesUUID)
                seriesRegistrationRepository.updateCountPublishedForSeries(product.seriesUUID)
                seriesRegistrationRepository.updateCountPendingForSeries(product.seriesUUID)
                seriesRegistrationRepository.updateCountDeclinedForSeries(product.seriesUUID)
            }
        }
    }
}
