package no.nav.hm.grunndata.register.supplier

import io.micronaut.runtime.event.ApplicationStartupEvent
import io.micronaut.runtime.event.annotation.EventListener
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking

@Singleton
class SupplierCacheInitializer(private val supplierRepository: SupplierRepository) {

    @EventListener
    fun onStartup(event: ApplicationStartupEvent) {
        LOG.info("Initializing SupplierRegistrationCache on application startup")
        runBlocking {
            SupplierRegistrationCache.refresh(supplierRepository)
        }
    }

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(SupplierCacheInitializer::class.java)
    }
}