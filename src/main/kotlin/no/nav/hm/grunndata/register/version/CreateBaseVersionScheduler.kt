package no.nav.hm.grunndata.register.version

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.leaderelection.LeaderOnly
import org.slf4j.LoggerFactory

@Singleton
@Requires(property = "baseversion.scheduler.enabled", value = "true")
open class CreateBaseVersionScheduler(private val createBaseVersionHandler: CreateBaseVersionHandler) {
    companion object {
        private val LOG = LoggerFactory.getLogger(CreateBaseVersionScheduler::class.java)
    }

    @LeaderOnly
    @Scheduled(fixedDelay = "4m")
    open fun scheduleProductExpirePublish() =
        runBlocking {
            LOG.info("Running ProductExpirePublishScheduler")
            createBaseVersionHandler.createVersionsWhereMissingForMigratedSuppliers()
        }
}
