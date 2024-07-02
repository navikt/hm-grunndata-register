package no.nav.hm.grunndata.register.product.attributes.produkttype

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.leaderelection.LeaderOnly

@Singleton
@Requires(property = "schedulers.enabled", value = "true")
open class ProduktTypeScheduler(private val produktTypeService: ProduktTypeService) {
    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(ProduktTypeScheduler::class.java)
    }

    @LeaderOnly
    @Scheduled(cron = "0 1 1 * * *")
    open fun importAndUpdateProduktType() {
        LOG.info("Running produkt type scheduler")
        runBlocking {
            produktTypeService.importAndUpdateDb()
        }

    }
}
