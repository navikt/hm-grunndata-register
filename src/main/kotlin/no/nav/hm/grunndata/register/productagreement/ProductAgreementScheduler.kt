package no.nav.hm.grunndata.register.productagreement

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.register.leaderelection.LeaderOnly
import org.slf4j.LoggerFactory

@Singleton
@Requires(property = "schedulers.enabled", value = "true")
open class ProductAgreementScheduler(private val productAgreementRegistrationService: ProductAgreementRegistrationService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(ProductAgreementScheduler::class.java)
    }

    @LeaderOnly
    @Scheduled(cron = "0 1 2 * * *")
    open fun connectProductAgreement() {

        LOG.info("Running product agreement connect scheduler")
        runBlocking {
            productAgreementRegistrationService.connectProductAgreementToProduct()
        }
    }

    @LeaderOnly
    @Scheduled(cron = "0 1 3 * * *")
    open fun deactivateProductAgreementsThatAreExpired() {
        LOG.info("Running product agreement deactivation scheduler")
        runBlocking {
            productAgreementRegistrationService.deactivateExpiredProductAgreements()
        }
    }

}