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
    private val serviceAgreementImportExcel: ServiceAgreementImportExcel,
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
                val (agreement, catalogimports) = catalogImportService.mapExcelDTOToCatalogImport(
                    catalogFile.catalogList,
                    supplierId
                )

                val catalogImportResult = catalogImportService.convertAndCreateCatalogImportResult(catalogimports, forceUpdate)

                val productImportResult = CatalogImportResult(catalogImportResult.insertedList.filter { it.isProduct()},
                    catalogImportResult.updatedList.filter { it.isProduct()} , catalogImportResult.deactivatedList.filter { it.isProduct()})
                LOG.info("Product import result has inserted: ${productImportResult.insertedList.size} updated: ${productImportResult.updatedList.size} " +
                        "deactivated: ${productImportResult.deactivatedList.size}")

                catalogImportService.handleNewProductsOrChangedSupplierRefFromCatalogImport(
                    productImportResult,
                    adminAuthentication
                )
                val productAgreementMappedResultLists = productAgreementImportExcelService.mapToProductAgreementImportResult(productImportResult, agreement, adminAuthentication, supplierId)
                productAgreementImportExcelService.persistResult( productAgreementMappedResultLists)
                catalogImportService.persistCatalogImportResult(productImportResult)

                val serviceImportResult = CatalogImportResult(catalogImportResult.insertedList.filter  { it.isService() },
                    catalogImportResult.updatedList.filter { it.isService() },
                    catalogImportResult.deactivatedList.filter { it.isService() })
                LOG.info("Service import result has inserted: ${serviceImportResult.insertedList.size} updated: ${serviceImportResult.updatedList.size} " +
                        "deactivated: ${serviceImportResult.deactivatedList.size}")

                catalogImportService.handleNewServices(serviceImportResult, adminAuthentication)

                val serviceAgreementImportResul = serviceAgreementImportExcel.mapToServiceAgreementImportResult(serviceImportResult, agreement,  adminAuthentication, supplierId)
                LOG.info("Persisting service and agreements from excel import")
                serviceAgreementImportExcel.persistResult(serviceAgreementImportResul)
                catalogImportService.persistCatalogImportResult(serviceImportResult)
                val updatedCatalogFile =
                    catalogFileRepository.update(
                        catalogFile.copy(
                            status = CatalogFileStatus.DONE,
                            updated = LocalDateTime.now()
                        )
                    )
                LOG.info("Finished saving with inserted: ${productAgreementMappedResultLists.insertList.size} updated: ${productAgreementMappedResultLists.updateList.size} " +
                        "deactivated: ${productAgreementMappedResultLists.deactivateList.size} for catalog file id: ${updatedCatalogFile.id} with name: ${updatedCatalogFile.fileName}")
                productAgreementMappedResultLists
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