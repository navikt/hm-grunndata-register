package no.nav.hm.grunndata.register.series

import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.LeaderElection
import org.slf4j.LoggerFactory

@Singleton
class SeriesScheduler(private val seriesStateHandler: SeriesStateHandler,
                      private val leaderElection: LeaderElection) {
    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesScheduler::class.java)
    }

    @Scheduled(fixedDelay = "30m")
    fun syncronizeProductWithSeries() {
        if (leaderElection.isLeader()) {
            LOG.info("Running syncronizeProductWithSeries scheduler")
            runBlocking {
                seriesStateHandler.syncronizeProductWithSeries()
            }
        }
    }

    @Scheduled(fixedDelay = "1h")
    fun findEmptySeriesAndDelete() {
        if (leaderElection.isLeader()) {
            LOG.info("Running syncronizeProductWithSeries scheduler")
            runBlocking {
                seriesStateHandler.findEmptyAndDeleteSeries()
            }
        }
    }


}