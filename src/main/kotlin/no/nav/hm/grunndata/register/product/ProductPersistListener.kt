package no.nav.hm.grunndata.register.product

import io.micronaut.context.annotation.Factory
import io.micronaut.data.event.listeners.PostPersistEventListener
import io.micronaut.data.event.listeners.PostRemoveEventListener
import io.micronaut.data.event.listeners.PostUpdateEventListener
import jakarta.inject.Singleton
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.series.SeriesRegistrationRepository
import org.slf4j.LoggerFactory

@Factory
class ProductPersistListener(private val seriesRegistrationRepository: SeriesRegistrationRepository,
                             private val productRegistrationVersionService: ProductRegistrationVersionService) {
    companion object {
        private val LOG = LoggerFactory.getLogger(ProductPersistListener::class.java)
    }

    @Singleton
    fun afterProductPersist(): PostPersistEventListener<ProductRegistration> {
        return PostPersistEventListener { product: ProductRegistration ->
            runBlocking {
                LOG.debug("ProductRegistration ${product.id} inserted for series: ${product.seriesUUID}")
                insertProductVersion(product)
                updateSeriesCounts(product.seriesUUID)
            }
        }
    }

    @Singleton
    fun afterProductUpdate(): PostUpdateEventListener<ProductRegistration> {
        return PostUpdateEventListener { product: ProductRegistration ->
            runBlocking {
                LOG.debug("ProductRegistration ${product.id} updated for series: ${product.seriesUUID}")
                insertProductVersion(product)
                updateSeriesCounts(product.seriesUUID)
            }
        }
    }

    @Singleton
    fun afterProductDelete(): PostRemoveEventListener<ProductRegistration> {
        return PostRemoveEventListener { product: ProductRegistration ->
            runBlocking {
                LOG.debug("ProductRegistration ${product.id} deleted for series: ${product.seriesUUID}")
                updateSeriesCounts(product.seriesUUID)
            }
        }
    }

    private suspend fun updateSeriesCounts(seriesUUID: UUID) {
        seriesRegistrationRepository.updateCountForSeries(seriesUUID)
        seriesRegistrationRepository.resetCountStatusesForSeries(seriesUUID)
        seriesRegistrationRepository.updateCountDraftsForSeries(seriesUUID)
        seriesRegistrationRepository.updateCountPublishedForSeries(seriesUUID)
        seriesRegistrationRepository.updateCountPendingForSeries(seriesUUID)
        seriesRegistrationRepository.updateCountDeclinedForSeries(seriesUUID)
    }

    private suspend fun insertProductVersion(product: ProductRegistration) {
        productRegistrationVersionService.save(product.toVersion())
    }

    private fun ProductRegistration.toVersion(): ProductRegistrationVersion = ProductRegistrationVersion(
            productId = this.id,
            version = this.version,
            draftStatus = this.draftStatus,
            adminStatus = this.adminStatus,
            status = this.registrationStatus,
            updated = this.updated,
            productRegistration = this,
            updatedBy = this.updatedBy
        )
}
