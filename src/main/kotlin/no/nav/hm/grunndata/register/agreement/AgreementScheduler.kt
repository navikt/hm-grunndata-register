package no.nav.hm.grunndata.register.agreement

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.micronaut.leaderelection.LeaderOnly
import org.slf4j.LoggerFactory

@Singleton
@Requires(property = "schedulers.enabled", value = "true")
open class AgreementScheduler(
    private val agreementExpiration: AgreementExpiration,
    private val agreementPublish: AgreementPublish,
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(AgreementScheduler::class.java)
    }

    @LeaderOnly
    @Scheduled(cron = "0 30 * * * *")
    open fun handleExpiredAgreements() {
        LOG.info("Running expiration agreement scheduler")
        runBlocking {
            agreementExpiration.expiredAgreements()
        }
    }

    @LeaderOnly
    @Scheduled(cron = "0 45 * * * *")
    open fun handlePublishAgreements() {
        LOG.info("Running publish agreement scheduler")
        runBlocking {
            agreementPublish.publishAgreements()
        }
    }

}