package no.nav.hm.grunndata.register.product.attributes.digitalsoknad

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.leaderelection.LeaderOnly

@Singleton
@Requires(property = "schedulers.enabled", value = "true")
open class DigitalSoknadScheduler(private val digitalSoknadService: DigitalSoknadService) {
    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(DigitalSoknadScheduler::class.java)
    }

    @LeaderOnly
    @Scheduled(cron = "0 1 1 * * *")
    open fun importAndUpdateDigitalSoknad() {
        LOG.info("Running digital soknad scheduler")
        runBlocking {
            digitalSoknadService.importAndUpdateDb()
        }

    }
}
