package no.nav.hm.grunndata.register.product

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.leaderelection.LeaderOnly
import org.slf4j.LoggerFactory

@Singleton
@Requires(property = "schedulers.enabled", value = "true")
open class ProductExpirePublishScheduler(private val productExpirePublishHandler: ProductExpirePublishHandler) {

    companion object {
        private val LOG = LoggerFactory.getLogger(ProductExpirePublishScheduler::class.java)
    }
    @LeaderOnly
    @Scheduled(fixedDelay = "15m")
    open fun scheduleProductExpirePublish() = runBlocking {
        LOG.info("Running ProductExpirePublishScheduler")
        productExpirePublishHandler.expiredProducts()
        productExpirePublishHandler.publishProducts()
    }
}