package no.nav.hm.grunndata.register.news

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.NewsStatus
import no.nav.hm.grunndata.register.LeaderElection
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Singleton
@Requires(property = "schedulers.enabled", value = "true")
open class NewsScheduler(
    private val leaderElection: LeaderElection,
    private val newsRegistrationService: NewsRegistrationService
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(NewsScheduler::class.java)
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    open fun deactiveNews() {
        runBlocking {
            if (leaderElection.isLeader()) {
                val expired = newsRegistrationService.findToBeExpired(
                    status = NewsStatus.ACTIVE,
                    expired = LocalDateTime.now()
                )
                LOG.info("Found ${expired.size} news to deactivate")
                expired.forEach { newsRegistrationService.update(it.copy(status = NewsStatus.INACTIVE)) }
            }
        }
    }

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    open fun activateNews() {
        runBlocking {
            if (leaderElection.isLeader()) {
                val toActivate = newsRegistrationService.findByToBePublished(
                    status = NewsStatus.INACTIVE,
                    expired = LocalDateTime.now(),
                    published = LocalDateTime.now()
                )
                LOG.info("Found ${toActivate.size} news to activate")
                toActivate.forEach { newsRegistrationService.update(it.copy(status = NewsStatus.ACTIVE)) }
            }
        }
    }

}