package no.nav.hm.grunndata.register.event

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.leaderelection.LeaderElection
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Singleton
@Requires(property = "schedulers.enabled", value = "true")
class EventItemScheduler(
    private val eventItemService: EventItemService,
    private val leaderElection: LeaderElection
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(EventItemScheduler::class.java)
    }

    @Scheduled(fixedDelay = "15s")
    fun sendEventItemScheduler() {
        runBlocking {
            if (leaderElection.isLeader()) {
                val items = eventItemService.getAllPendingStatus()
                LOG.info("Running sendEventItemScheduler with ${items.size} items")
                items.forEach {
                    LOG.info("sending event ${it.oid} with type ${it.type}")
                    eventItemService.sendRapidEvent(it)
                }
            }
        }
    }

    @Scheduled(cron = "0 0 0 * * ?")
    fun deleteSentItemScheduler() {
        runBlocking {
            if (leaderElection.isLeader()) {
                val before = LocalDateTime.now().minusDays(30)
                val deleted = eventItemService.deleteByStatusAndUpdatedBefore(EventItemStatus.SENT,before)
                LOG.info("Running deleteSentItemScheduler with $deleted items before: $before")
            }
        }
    }
}