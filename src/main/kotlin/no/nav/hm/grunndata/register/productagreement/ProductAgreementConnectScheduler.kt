package no.nav.hm.grunndata.register.productagreement

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.leaderelection.LeaderOnly
import org.slf4j.LoggerFactory

@Singleton
@Requires(property = "schedulers.enabled", value = "true")
open class ProductAgreementConnectScheduler(private val productAgreementRegistrationService: ProductAgreementRegistrationService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(ProductAgreementConnectScheduler::class.java)
    }

    @LeaderOnly
    @Scheduled(cron = "0 1 2 * * *")
    open fun connectProductAgreement() {

        LOG.info("Running product agreement connect scheduler")
        runBlocking {
            productAgreementRegistrationService.connectProductAgreementToProduct()
        }
    }

}