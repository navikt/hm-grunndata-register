package no.nav.hm.grunndata.register.productagreement

import io.micronaut.scheduling.annotation.Scheduled
import io.micronaut.security.authentication.ClientAuthentication
import jakarta.inject.Singleton
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.CatalogFileStatus
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.catalog.CatalogFileDTO
import no.nav.hm.grunndata.register.catalog.CatalogFileEventHandler
import no.nav.hm.grunndata.register.catalog.CatalogFileRepository
import no.nav.hm.grunndata.register.leaderelection.LeaderOnly
import no.nav.hm.grunndata.register.security.Roles

@Singleton
open class CatalogFileToProductAgreementScheduler(private val catalogFileRepository: CatalogFileRepository,
                                             private val catalogFileEventHandler: CatalogFileEventHandler,
                                             private val productAgreementImportExcelService: ProductAgreementImportExcelService) {

    @LeaderOnly
    @Scheduled(cron = "0 * * * * *")
    open fun scheduleCatalogFileToProductAgreement() {
        runBlocking {
        val catalogFile = catalogFileRepository.findOneByStatus(CatalogFileStatus.PENDING)
            if (catalogFile != null) {
                try {
                    LOG.info("Got catalog file with id: ${catalogFile.id} with name: ${catalogFile.fileName}")
                    val adminAuthentication =
                        ClientAuthentication(catalogFile.updatedByUser, mapOf("roles" to listOf(Roles.ROLE_ADMIN)))
                    val productAgreementResult = productAgreementImportExcelService.mapToProductAgreementImportResult(
                        catalogFile.catalogList,
                        adminAuthentication,
                        catalogFile.supplierId,
                        false
                    )
                    LOG.info("Finished, saving result")
                    val catalogFile = catalogFileRepository.update(catalogFile.copy(status = CatalogFileStatus.DONE))
                    val dto = CatalogFileDTO(
                        id = catalogFile.id,
                        fileName = catalogFile.fileName,
                        fileSize = catalogFile.fileSize,
                        orderRef = catalogFile.orderRef,
                        supplierId = catalogFile.supplierId,
                        updatedByUser = catalogFile.updatedByUser,
                        created = catalogFile.created,
                        updated = catalogFile.updated,
                        status = catalogFile.status
                    )
                    catalogFileEventHandler.queueDTORapidEvent(dto, eventName = EventName.registeredCatalogfileV1)
                }

                catch (e: Exception) {
                    LOG.error("Error while processing catalog file with id: ${catalogFile.id} with name: ${catalogFile.fileName}", e)
                    catalogFileRepository.update(catalogFile.copy(status = CatalogFileStatus.ERROR))
                }
            }
        }
    }

    @LeaderOnly
    @Scheduled(cron = "0 0 3 * * *")
    open fun deleteOldCatalogFiles() {
        runBlocking {
            LOG.info("Deleting old catalog files")
            catalogFileRepository.deleteByStatusAndCreatedBefore(CatalogFileStatus.DONE, LocalDateTime.now().minusDays(30))
        }
    }

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(CatalogFileToProductAgreementScheduler::class.java)
    }


}