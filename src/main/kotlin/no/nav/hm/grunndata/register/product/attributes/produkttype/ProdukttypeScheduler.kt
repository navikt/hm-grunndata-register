package no.nav.hm.grunndata.register.product.attributes.produkttype

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.micronaut.leaderelection.LeaderOnly

@Singleton
@Requires(property = "schedulers.enabled", value = "true")
open class ProdukttypeScheduler(private val produkttypeService: ProdukttypeService) {
    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(ProdukttypeScheduler::class.java)
    }

    @LeaderOnly
    @Scheduled(cron = "0 1 1 * * *")
    open fun importAndUpdateProdukttype() {
        LOG.info("Running produkttype scheduler")
        runBlocking {
            produkttypeService.importAndUpdateDb()
        }
    }
}
