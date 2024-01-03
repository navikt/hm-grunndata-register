package no.nav.hm.grunndata.db.agreement

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.LeaderElection
import no.nav.hm.grunndata.register.agreement.AgreementPublish
import org.slf4j.LoggerFactory

@Singleton
@Requires(property = "schedulers.enabled", value = "true")
class AgreementScheduler(
    private val agreementExpiration: AgreementExpiration,
    private val agreementPublish: AgreementPublish,
    private val leaderElection: LeaderElection
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(AgreementScheduler::class.java)
    }

    @Scheduled(cron = "0 30 1 * * *")
    fun handleExpiredAgreements() {
        if (leaderElection.isLeader()) {
            LOG.info("Running expiration agreement scheduler")
            runBlocking {
                agreementExpiration.expiredAgreements()
            }
        }
    }

    @Scheduled(cron = "0 30 2 * * *")
    fun handlePublishAgreements() {
        if (leaderElection.isLeader()) {
            LOG.info("Running publish agreement scheduler")
            runBlocking {
                agreementPublish.publishAgreements()
            }
        }
    }

}