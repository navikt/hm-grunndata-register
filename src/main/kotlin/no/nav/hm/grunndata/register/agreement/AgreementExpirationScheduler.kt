package no.nav.hm.grunndata.db.agreement

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.LeaderElection
import org.slf4j.LoggerFactory

@Singleton
@Requires(property = "schedulers.enabled", value = "true")
class AgreementExpirationScheduler(
    private val agreementExpiration: AgreementExpiration,
    private val leaderElection: LeaderElection
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(AgreementExpirationScheduler::class.java)
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
}