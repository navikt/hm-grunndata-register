package no.nav.hm.grunndata.register.event

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.LeaderElection
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationEventHandler
import no.nav.hm.grunndata.register.bestillingsordning.BestillingsordningEventHandler
import no.nav.hm.grunndata.register.product.ProductRegistrationEventHandler
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationEventHandler
import no.nav.hm.grunndata.register.series.SeriesRegistrationEventHandler
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationHandler
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Singleton
@Requires(property = "schedulers.enabled", value = "true")
class EventItemScheduler(
    private val eventItemService: EventItemService,
    private val leaderElection: LeaderElection,
    private val agreementRegistrationEventHandler: AgreementRegistrationEventHandler,
    private val productRegistrationEventHandler: ProductRegistrationEventHandler,
    private val seriesRegistrationEventHandler: SeriesRegistrationEventHandler,
    private val supplierRegistrationHandler: SupplierRegistrationHandler,
    private val productAgreementRegistrationHandler: ProductAgreementRegistrationEventHandler,
    private val bestillingsordningEventHandler: BestillingsordningEventHandler
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
                    when (it.type) {
                        EventItemType.AGREEMENT -> agreementRegistrationEventHandler.sendRapidEvent(it)
                        EventItemType.PRODUCT -> productRegistrationEventHandler.sendRapidEvent(it)
                        EventItemType.SERIES -> seriesRegistrationEventHandler.sendRapidEvent(it)
                        EventItemType.SUPPLIER -> supplierRegistrationHandler.sendRapidEvent(it)
                        EventItemType.PRODUCTAGREEMENT -> productAgreementRegistrationHandler.sendRapidEvent(it)
                        EventItemType.BESTILLINGSORDNING -> bestillingsordningEventHandler.sendRapidEvent(it)
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