package no.nav.hm.grunndata.register.series

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.leaderelection.LeaderOnly

@Singleton
@Requires(property = "schedulers.enabled", value = "true")
open class SeriesRegistrationVersionScheduler(
    private val seriesRegistrationVersionService: SeriesRegistrationVersionService) {

    @LeaderOnly
    @Scheduled(cron = "0 0 0 * * *")
    open fun deleteOldSeriesRegistrationVersions() = runBlocking {
        seriesRegistrationVersionService.deleteOldVersions()
    }

}