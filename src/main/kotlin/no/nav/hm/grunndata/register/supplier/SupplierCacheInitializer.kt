package no.nav.hm.grunndata.register.supplier

import io.micronaut.runtime.event.ApplicationStartupEvent
import io.micronaut.runtime.event.annotation.EventListener
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking

@Singleton
class SupplierCacheInitializer(private val supplierRepository: SupplierRepository) {

    @EventListener
    fun onStartup(event: ApplicationStartupEvent) {
        runBlocking {
            SupplierRegistrationCache.refresh(supplierRepository)
        }
    }
}