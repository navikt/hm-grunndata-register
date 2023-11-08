package no.nav.hm.grunndata.register.event

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.LeaderElection
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationService
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.series.SeriesRegistrationService
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Singleton
@Requires(property = "schedulers.enabled", value = "true")
class EventItemScheduler(
    private val eventItemService: EventItemService,
    private val leaderElection: LeaderElection,
    private val agreementRegistrationService: AgreementRegistrationService,
    private val productRegistrationService: ProductRegistrationService,
    private val seriesRegistrationService: SeriesRegistrationService,
    private val supplierRegistrationService: SupplierRegistrationService
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(EventItemScheduler::class.java)
    }

    @Scheduled(fixedDelay = "1m")
    fun sendEventItemScheduler() {
        runBlocking {
            if (leaderElection.isLeader()) {
                val items = eventItemService.getAllPendingStatus()
                LOG.info("Running sendEventItemScheduler with ${items.size} items")
                items.forEach {
                    LOG.info("sending event ${it.oid} with type ${it.type}")
                    when (it.type) {
                        EventItemType.AGREEMENT -> agreementRegistrationService.handleEventItem(it)
                        EventItemType.PRODUCT -> productRegistrationService.handleEventItem(it)
                        EventItemType.SERIES -> seriesRegistrationService.handleEventItem(it)
                        EventItemType.SUPPLIER -> supplierRegistrationService.handleEventItem(it)
                    }
                    eventItemService.setEventItemStatusToSent(it)
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