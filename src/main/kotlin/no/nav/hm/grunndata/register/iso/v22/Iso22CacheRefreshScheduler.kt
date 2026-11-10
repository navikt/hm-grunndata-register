package no.nav.hm.grunndata.register.iso.v22

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking

// Ikke @LeaderOnly: cachen ligger i minnet på hver pod og må friskes opp overalt.
@Singleton
@Requires(property = "schedulers.enabled", value = "true")
open class Iso22CacheRefreshScheduler(private val iso22Service: Iso22Service) {

    @Scheduled(fixedDelay = "5m", initialDelay = "5m")
    open fun refreshIso22Cache() {
        runBlocking { iso22Service.refresh() }
    }
}
