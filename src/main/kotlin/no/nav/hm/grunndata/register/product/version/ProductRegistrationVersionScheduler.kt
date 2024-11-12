package no.nav.hm.grunndata.register.product.version

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.leaderelection.LeaderOnly

@Singleton
@Requires(property = "schedulers.enabled", value = "true")
open class ProductRegistrationVersionScheduler(
    private val productRegistrationVersionService: ProductRegistrationVersionService
) {

    @LeaderOnly
    @Scheduled(cron = "0 0 1 * * *")
    open fun deleteOldSeriesRegistrationVersions() = runBlocking {
        productRegistrationVersionService.deleteOldVersions()
    }

}