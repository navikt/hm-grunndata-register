package no.nav.hm.grunndata.register.series

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.leaderelection.LeaderOnly
import org.slf4j.LoggerFactory

@Singleton
@Requires(property = "schedulers.enabled", value = "true")
open class SeriesScheduler(
    private val seriesStateHandler: SeriesStateHandler,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesScheduler::class.java)
    }

    @LeaderOnly
    @Scheduled(fixedDelay = "6h")
    open fun syncronizeProductWithSeries() {
        LOG.info("Running syncronizeProductWithSeries scheduler")
        runBlocking {
            seriesStateHandler.findProductsThatHasNoSeries()
        }
    }

}