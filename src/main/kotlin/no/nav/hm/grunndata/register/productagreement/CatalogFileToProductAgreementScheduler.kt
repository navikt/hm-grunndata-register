package no.nav.hm.grunndata.register.productagreement

import io.micronaut.context.annotation.Value
import io.micronaut.scheduling.annotation.Scheduled
import io.micronaut.security.authentication.ClientAuthentication
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.CatalogFileStatus
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.catalog.CatalogFileDTO
import no.nav.hm.grunndata.register.catalog.CatalogFileEventHandler
import no.nav.hm.grunndata.register.catalog.CatalogFileRepository
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.micronaut.leaderelection.LeaderOnly
import java.time.LocalDateTime

@Singleton
open class CatalogFileToProductAgreementScheduler(
    private val catalogFileRepository: CatalogFileRepository,
    private val catalogFileEventHandler: CatalogFileEventHandler,
    private val productRegistrationRepository: ProductRegistrationRepository,
    private val productAgreementImportExcelService: ProductAgreementImportExcelService,
    @Value("\${CATALOG_IMPORT_FORCE_UPDATE:false}") private val forceUpdate: Boolean = false
) {

    @LeaderOnly
    @Scheduled(cron = "0 * * * * *")
    open fun scheduleCatalogFileToProductAgreement(): ProductAgreementImportResult? = runBlocking {
            catalogFileRepository.findOneByStatusOrderByCreatedAsc(CatalogFileStatus.PENDING)?.let { catalogFile ->
                try {
                    LOG.info("Got catalog file with id: ${catalogFile.id} with name: ${catalogFile.fileName} to process with forceUpdate: $forceUpdate")
                    val adminAuthentication =
                        ClientAuthentication(catalogFile.updatedByUser, mapOf("roles" to listOf(Roles.ROLE_ADMIN)))
                    val result = productAgreementImportExcelService.mapToProductAgreementImportResult(
                        catalogFile.catalogList,
                        adminAuthentication,
                        catalogFile.supplierId,
                        false,
                        forceUpdate
                    )
                    LOG.info("Finished, saving result")
                    val updatedCatalogFile =
                        catalogFileRepository.update(catalogFile.copy(status = CatalogFileStatus.DONE, updated = LocalDateTime.now()))
                    val dto = CatalogFileDTO(
                        id = updatedCatalogFile.id,
                        fileName = updatedCatalogFile.fileName,
                        fileSize = updatedCatalogFile.fileSize,
                        orderRef = updatedCatalogFile.orderRef,
                        supplierId = updatedCatalogFile.supplierId,
                        updatedByUser = updatedCatalogFile.updatedByUser,
                        created = updatedCatalogFile.created,
                        updated = updatedCatalogFile.updated,
                        status = updatedCatalogFile.status
                    )
                    catalogFileEventHandler.queueDTORapidEvent(dto, eventName = EventName.registeredCatalogfileV1)
                    result
                } catch (e: Exception) {
                    LOG.error(
                        "Error while processing catalog file with id: ${catalogFile.id} with name: ${catalogFile.fileName}",
                        e
                    )
                    catalogFileRepository.update(catalogFile.copy(status = CatalogFileStatus.ERROR, updated = LocalDateTime.now()))
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
        private val LOG = org.slf4j.LoggerFactory.getLogger(CatalogFileToProductAgreementScheduler::class.java)
    }

}