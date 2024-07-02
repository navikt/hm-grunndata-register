package no.nav.hm.grunndata.register.product.attributes.digitalsoknadsortiment

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.leaderelection.LeaderOnly

@Singleton
@Requires(property = "schedulers.enabled", value = "true")
open class DigitalSoknadSortimentScheduler(private val digitalSoknadSortimentService: DigitalSoknadSortimentService) {
    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(DigitalSoknadSortimentScheduler::class.java)
    }

    @LeaderOnly
    @Scheduled(cron = "0 1 1 * * *")
    open fun importAndUpdateDigitalSoknadSortiment() {
        LOG.info("Running digital soknad sortiment scheduler")
        runBlocking {
            digitalSoknadSortimentService.importAndUpdateDb()
        }

    }
}
