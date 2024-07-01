package no.nav.hm.grunndata.register.product.attributes.bestillingsordning

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.leaderelection.LeaderOnly

@Singleton
@Requires(property = "schedulers.enabled", value = "true")
open class BestillingsordningScheduler(private val bestillingsordningService: BestillingsordningService) {

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(BestillingsordningScheduler::class.java)
    }

    @LeaderOnly
    @Scheduled(cron = "0 1 1 * * *")
    open fun importAndUpdateBestillingsOrdning() {
        LOG.info("Running bestillingsordning scheduler")
        runBlocking {
            bestillingsordningService.importAndUpdateDb()
        }

    }
}
