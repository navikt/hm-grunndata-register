package no.nav.hm.grunndata.register.accessory

import io.micronaut.data.model.Pageable
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.CatalogFileStatus
import no.nav.hm.grunndata.register.catalog.CatalogFileRepository
import no.nav.hm.grunndata.register.leaderelection.LeaderOnly
import org.slf4j.LoggerFactory


@Singleton
open class CompatibleWithConnectScheduler(private val catalogFileRepository: CatalogFileRepository,
                                          private val compatibleWithFinder: CompatibleWithFinder) {

    //@Scheduled(cron = "0 0 0 * * ?")
    //@LeaderOnly
    open fun connectCompatibleWith() {
        runBlocking {
            catalogFileRepository.findManyByStatus(CatalogFileStatus.DONE, pageable = Pageable.UNPAGED).forEach { catalogFile ->
                LOG.info("Connecting catalog file with orderRef: ${catalogFile.orderRef} with name: ${catalogFile.fileName}")
                compatibleWithFinder.connectWithOrderRef(catalogFile.orderRef)
                catalogFileRepository.updateStatusById(catalogFile.id, CatalogFileStatus.FINISHED)
            }

        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CompatibleWithConnectScheduler::class.java)
    }

}