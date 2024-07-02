package no.nav.hm.grunndata.register.version

import io.micronaut.context.annotation.Requires
import io.micronaut.context.event.StartupEvent
import io.micronaut.runtime.event.annotation.EventListener
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

    @EventListener
    @LeaderOnly
    open fun init(event: StartupEvent?) {
        runBlocking {
            LOG.info("Running ProductExpirePublishScheduler")
            createBaseVersionHandler.createVersionsWhereMissingForMigratedSuppliers()
        }
    }
}
