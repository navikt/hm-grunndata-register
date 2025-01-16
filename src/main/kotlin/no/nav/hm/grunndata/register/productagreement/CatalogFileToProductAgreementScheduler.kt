package no.nav.hm.grunndata.register.productagreement

import io.micronaut.scheduling.annotation.Scheduled
import io.micronaut.security.authentication.ClientAuthentication
import jakarta.inject.Singleton
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.CatalogFileStatus
import no.nav.hm.grunndata.register.catalog.CatalogFileRepository
import no.nav.hm.grunndata.register.security.Roles

@Singleton
class CatalogFileToProductAgreementScheduler(private val catalogFileRepository: CatalogFileRepository,
                                             private val productAgreementImportExcelService: ProductAgreementImportExcelService) {

    @Scheduled(cron = "0 * * * * *")
    fun scheduleCatalogFileToProductAgreement() {
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
                    catalogFileRepository.update(catalogFile.copy(status = CatalogFileStatus.DONE))
                }
                catch (e: Exception) {
                    LOG.error("Error while processing catalog file with id: ${catalogFile.id} with name: ${catalogFile.fileName}", e)
                    catalogFileRepository.update(catalogFile.copy(status = CatalogFileStatus.ERROR))
                }
            }
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    fun deleteOldCatalogFiles() {
        runBlocking {
            LOG.info("Deleting old catalog files")
            catalogFileRepository.deleteByStatusAndCreatedBefore(CatalogFileStatus.DONE, LocalDateTime.now().minusDays(30))
        }
    }

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(CatalogFileToProductAgreementScheduler::class.java)
    }


}