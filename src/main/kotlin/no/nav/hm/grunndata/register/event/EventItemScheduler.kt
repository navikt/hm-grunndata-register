package no.nav.hm.grunndata.register.event

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.leaderelection.LeaderOnly
import org.slf4j.LoggerFactory

@Singleton
@Requires(property = "schedulers.enabled", value = "true")
open class EventItemScheduler(
    private val eventItemService: EventItemService
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(EventItemScheduler::class.java)
    }

    @LeaderOnly
    @Scheduled(fixedDelay = "15s")
    open fun sendEventItemScheduler() {
        runBlocking {
            val items = eventItemService.findByStatusOrderByUpdatedAsc()
            LOG.info("Running sendEventItemScheduler with ${items.size} items")
            items.forEach {
                LOG.info("sending event ${it.oid} with type ${it.type} and ${it.updated}")
                eventItemService.sendRapidEvent(it)
            }
        }
    }

    @LeaderOnly
    @Scheduled(cron = "0 0 0 * * ?")
    open fun deleteSentItemScheduler() {
        runBlocking {
            val before = LocalDateTime.now().minusDays(30)
            val deleted = eventItemService.deleteByStatusAndUpdatedBefore(EventItemStatus.SENT, before)
            LOG.info("Running deleteSentItemScheduler with $deleted items before: $before")
        }
    }
}