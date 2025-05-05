package no.nav.hm.grunndata.register.accessory

import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.micronaut.leaderelection.LeaderOnly
import org.slf4j.LoggerFactory


@Singleton
open class CompatibleWithConnectScheduler(
    private val compatibleWithFinder: CompatibleWithFinder
) {

    @Scheduled(cron = "0 35 00 * * ?")
    @LeaderOnly
    open fun connectCompatibleWith() {
        runBlocking {
            LOG.info("Disabled this job while testing AI")
            //compatibleWithFinder.connectAllOrdersNotConnected()
        }
    }


    companion object {
        private val LOG = LoggerFactory.getLogger(CompatibleWithConnectScheduler::class.java)
    }

}