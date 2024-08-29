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

    // todo: deactivating this as it removes series in draft also
//    @LeaderOnly
//    @Scheduled(fixedDelay = "1h")
//    open fun findEmptySeriesAndDelete() {
//        LOG.info("Running find empty series scheduler")
//        runBlocking {
//            seriesStateHandler.findEmptyAndDeleteSeries()
//        }
//    }

//    @LeaderOnly Disable this scheduler, we should not need anymore
//    @Scheduled(cron = "0 35 2 * * ?")
//    open fun copyMediaFromProductsToSeries() {
//        LOG.info("Running copy media from products to series scheduler")
//        runBlocking {
//            seriesStateHandler.copyMediaFromProductsToSeries()
//        }
//    }

//    @LeaderOnly
//    @Scheduled(fixedDelay = "3h")
//    open fun fixEmptyCategorySeries() {
//        LOG.info("Running fix empty category series scheduler")
//        runBlocking {
//            seriesStateHandler.findEmptyCategorySeriesAndPopulateWithProductsData()
//        }
//    }

}