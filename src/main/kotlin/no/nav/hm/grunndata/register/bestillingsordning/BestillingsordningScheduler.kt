package no.nav.hm.grunndata.register.bestillingsordning

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.LeaderElection


@Singleton
@Requires(property = "schedulers.enabled", value = "true")
class BestillingsordningScheduler(private val leaderElection: LeaderElection,
                                  private val bestillingsordningService: BestillingsordningService) {

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(BestillingsordningScheduler::class.java)
    }

    @Scheduled(cron = "0 1 1 * * *")
    fun importAndUpdateBestillingsOrdning() {
        if (leaderElection.isLeader()) {
            LOG.info("Running bestillingsordning scheduler")
            runBlocking {
                bestillingsordningService.importAndUpdateDb()
            }
        }
    }
}