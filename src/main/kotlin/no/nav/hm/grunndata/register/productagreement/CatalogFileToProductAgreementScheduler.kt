package no.nav.hm.grunndata.register.productagreement

import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.scheduling.annotation.Scheduled
import io.micronaut.security.authentication.ClientAuthentication
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.catalog.CatalogFileRepository
import no.nav.hm.grunndata.register.catalog.CatalogFileStatus
import no.nav.hm.grunndata.register.catalog.CatalogImportResult
import no.nav.hm.grunndata.register.catalog.CatalogImportService
import no.nav.hm.grunndata.register.product.isAdmin
import no.nav.hm.grunndata.register.security.Roles

@Singleton
class CatalogFileToProductAgreementScheduler(private val catalogFileRepository: CatalogFileRepository,
                                             private val catalogImportService: CatalogImportService,
                                             private val productAgreementImportExcelService: ProductAgreementImportExcelService,
    private val productAccessorySparePartAgreementHandler: ProductAccessorySparePartAgreementHandler) {

    @Scheduled(cron = "0 * * * * *")
    fun scheduleCatalogFileToProductAgreement() {
        runBlocking {
        val catalogFile = catalogFileRepository.findOneByStatus(CatalogFileStatus.PENDING)
            if (catalogFile != null) {
                try {
                    LOG.info("Got catalog file with id: ${catalogFile.id} with name: ${catalogFile.fileName}")
                    val adminAuthentication =
                        ClientAuthentication(catalogFile.createdByUser, mapOf("roles" to listOf(Roles.ROLE_ADMIN)))
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

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(CatalogFileToProductAgreementScheduler::class.java)
    }


}