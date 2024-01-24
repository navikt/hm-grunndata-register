package no.nav.hm.grunndata.register.productagreement

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.LeaderElection
import org.slf4j.LoggerFactory

@Singleton
@Requires(property = "schedulers.enabled", value = "true")
class ProductAgreementConnectScheduler(private val leaderElection: LeaderElection,
                                       private val productAgreementRegistrationService: ProductAgreementRegistrationService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(ProductAgreementConnectScheduler::class.java)
    }

    @Scheduled(cron = "0 1 2 * * *")
    fun connectProductAgreement() {
        if (leaderElection.isLeader()) {
            LOG.info("Running product agreement connect scheduler")
            runBlocking {
                productAgreementRegistrationService.connectProductAgreementToProduct()
            }
        }
    }

}