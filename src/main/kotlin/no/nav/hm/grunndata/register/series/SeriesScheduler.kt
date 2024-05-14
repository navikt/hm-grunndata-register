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
    @Scheduled(fixedDelay = "30m")
    open fun syncronizeProductWithSeries() {
        LOG.info("Running syncronizeProductWithSeries scheduler")
        runBlocking {
            seriesStateHandler.findProductsThatHasNoSeries()
        }
    }

    @LeaderOnly
    @Scheduled(fixedDelay = "1h")
    open fun findEmptySeriesAndDelete() {
        LOG.info("Running find empty series scheduler")
        runBlocking {
            seriesStateHandler.findEmptyAndDeleteSeries()
        }
    }

    @LeaderOnly
    @Scheduled(cron = "0 35 2 * * ?")
    open fun copyMediaFromProductsToSeries() {
        LOG.info("Running copy media from products to series scheduler")
        runBlocking {
            seriesStateHandler.copyMediaFromProductsToSeries()
        }
    }

}