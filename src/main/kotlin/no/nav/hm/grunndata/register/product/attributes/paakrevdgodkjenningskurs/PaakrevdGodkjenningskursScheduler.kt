package no.nav.hm.grunndata.register.product.attributes.paakrevdgodkjenningskurs

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.micronaut.leaderelection.LeaderOnly

@Singleton
@Requires(property = "schedulers.enabled", value = "true")
open class PaakrevdGodkjenningskursScheduler(private val paakrevdGodkjenningskursService: PaakrevdGodkjenningskursService) {
    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(PaakrevdGodkjenningskursScheduler::class.java)
    }

    @LeaderOnly
    @Scheduled(cron = "0 30 4 * * *")
    open fun importAndUpdatePaakrevdGodkjenningskurs() {
        LOG.info("Running paakrevd godkjenningskurs scheduler")
        runBlocking {
            paakrevdGodkjenningskursService.importAndUpdateDb()
        }
    }
}
