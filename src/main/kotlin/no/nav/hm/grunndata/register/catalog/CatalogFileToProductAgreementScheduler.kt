package no.nav.hm.grunndata.register.catalog

import io.micronaut.context.annotation.Value
import io.micronaut.scheduling.annotation.Scheduled
import io.micronaut.security.authentication.ClientAuthentication
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.CatalogFileStatus
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.micronaut.leaderelection.LeaderOnly
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Singleton
open class CatalogFileToProductAgreementScheduler(
    private val catalogFileRepository: CatalogFileRepository,
    private val productRegistrationRepository: ProductRegistrationRepository,
    private val catalogImportService: CatalogImportService,
    private val productAgreementImportExcelService: ProductAgreementImportExcelService,
    @Value("\${catalog.import.force_update}") private val forceUpdate: Boolean
) {

    @LeaderOnly
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    open fun scheduleCatalogFileToProductAgreement(): ProductAgreementMappedResultLists? = runBlocking {
        catalogFileRepository.findOneByStatusOrderByCreatedAsc(CatalogFileStatus.PENDING)?.let { catalogFile ->
            try {
                LOG.info("Got catalog file with filename: ${catalogFile.fileName} with rows: ${catalogFile.catalogList.size} to process with forceUpdate: $forceUpdate")
                val supplierId = catalogFile.supplierId
                val adminAuthentication =
                    ClientAuthentication(catalogFile.updatedByUser, mapOf("roles" to listOf(Roles.ROLE_ADMIN)))
                val catalogImportResult = catalogImportService.mapExcelDTOToCatalogImportResult(catalogFile.catalogList, supplierId, forceUpdate)
                catalogImportService.handleNewOrChangedSupplierRefFromCatalogImport(
                    catalogImportResult,
                    adminAuthentication
                )

                val result = productAgreementImportExcelService.mapToProductAgreementImportResult(catalogImportResult, adminAuthentication, supplierId)
                val updatedCatalogFile =
                    catalogFileRepository.update(
                        catalogFile.copy(
                            status = CatalogFileStatus.DONE,
                            updated = LocalDateTime.now()
                        )
                    )
                LOG.info("Finished saving with inserted: ${result.insertList.size} updated: ${result.updateList.size} " +
                        "deactivated: ${result.deactivateList.size} for catalog file id: ${updatedCatalogFile.id} with name: ${updatedCatalogFile.fileName}")
                result
            } catch (e: Exception) {
                LOG.error(
                    "Error while processing catalog file with id: ${catalogFile.id} with name: ${catalogFile.fileName}",
                    e
                )
                catalogFileRepository.update(
                    catalogFile.copy(
                        status = CatalogFileStatus.ERROR,
                        updated = LocalDateTime.now(),
                        errorMessage = e.message
                    )
                )
                null
            }
        }
    }

    @LeaderOnly
    @Scheduled(cron = "0 0 2 * * *")
    open suspend fun findInconsistenciesBetweenFHCatalog() {
        val productsNotMatchAS =  productRegistrationRepository.findProductThatDoesNotMatchAgreementSparePartAccessory()
        productsNotMatchAS.forEach {
            LOG.error("Product: ${it.id} hmsnr: ${it.hmsArtNr} does not match agreement and spare part/accessory")
        }
        val productsNotMatchHmsNr = productRegistrationRepository.findProductThatDoesNotMatchAgreementHmsNr()
        productsNotMatchHmsNr.forEach {
            LOG.error("Product: ${it.id} hmsnr: ${it.hmsArtNr} does not match agreement hmsnr")
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CatalogFileToProductAgreementScheduler::class.java)
    }

}