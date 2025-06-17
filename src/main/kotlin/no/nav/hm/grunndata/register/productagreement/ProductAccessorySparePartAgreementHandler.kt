package no.nav.hm.grunndata.register.productagreement

import io.micronaut.security.authentication.Authentication
import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.product.ProductData
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.product.isAdmin
import no.nav.hm.grunndata.register.series.SeriesDataDTO
import no.nav.hm.grunndata.register.series.SeriesRegistration
import no.nav.hm.grunndata.register.series.SeriesRegistrationService
import org.slf4j.LoggerFactory
import java.util.*

@Singleton
class ProductAccessorySparePartAgreementHandler(
    private val productRegistrationService: ProductRegistrationService,
    private val seriesRegistrationService: SeriesRegistrationService,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(ProductAccessorySparePartAgreementHandler::class.java)
    }

    /**
     * This function will handle the products, accessory and spareparts in the productAgreement catalog.
     * create the series and the products if not exists and dryRun is false
     * returns a list of productAgreements with seriesUuid and productId
     * The products will have admin status PENDING and draft status DONE
     */
    suspend fun handleNewProductsInExcelImport(
        importResult: ProductAgreementMappedResultLists,
        authentication: Authentication?,
        dryRun: Boolean = true,
    ): ProductAgreementImportResult {
        val newProductAgreements = importResult.insertList
        if (newProductAgreements.isEmpty()) {
            return ProductAgreementImportResult(
                insertList = emptyList(),
                updateList = importResult.updateList,
                deactivateList = importResult.deactivateList,
                newSeries = emptyList(),
                newAccessoryParts = emptyList(),
                newProducts = emptyList(),
            )
        }
        val (mainProductAgreements, accessoryOrSpareParts) = newProductAgreements.partition { it.mainProduct }
        val supplierId = newProductAgreements.first().supplierId
        val groupedAccessoryOrSpareParts = groupInSeriesBasedOnSupplierRef(accessoryOrSpareParts)
        val groupedMainProducts = groupInSeriesBasedOnSupplierRef(mainProductAgreements)
        val createdAccessorSpareParts =
            createSeriesAndProductsIfNotExists(groupedAccessoryOrSpareParts, supplierId, authentication, dryRun)
        val createdMainProducts =
            createSeriesAndProductsIfNotExists(groupedMainProducts, supplierId, authentication, dryRun)
        return ProductAgreementImportResult(
            insertList = createdAccessorSpareParts.productAgreement + createdMainProducts.productAgreement,
            updateList = importResult.updateList,
            deactivateList = importResult.deactivateList,
            newSeries = createdMainProducts.newSeries + createdAccessorSpareParts.newSeries,
            newAccessoryParts = createdAccessorSpareParts.newProducts,
            newProducts = createdMainProducts.newProducts,
        )
    }

    private suspend fun createSeriesAndProductsIfNotExists(
        groupedProductAgreements: Map<String, List<ProductAgreementRegistrationDTO>>,
        supplierId: UUID,
        authentication: Authentication?,
        dryRun: Boolean,
    ): ProductAgreementImportResultData {
        val newSeries = mutableListOf<SeriesRegistration>()
        val withSeriesId =
            groupedProductAgreements.flatMap { (key, value) ->
                // check if in value list that all seriesUuid is null
                val noSeries = value.all { it.seriesUuid == null }
                val first = value.first()
                val seriesGroup =
                    if (noSeries) {
                        // create a new series for this group
                        val seriesId = UUID.randomUUID()
                        val series =
                            SeriesRegistration(
                                id = seriesId,
                                draftStatus = DraftStatus.DONE,
                                adminStatus = if (first.mainProduct) AdminStatus.PENDING else AdminStatus.APPROVED,
                                supplierId = supplierId,
                                title = first.title,
                                identifier = seriesId.toString(),
                                isoCategory = first.isoCategory ?: "0",
                                status = SeriesStatus.ACTIVE,
                                seriesData = SeriesDataDTO(),
                                text = first.title.trim(),
                                createdByUser = authentication?.name ?: "system",
                                updatedByUser = authentication?.name ?: "system",
                                createdByAdmin = authentication?.isAdmin() ?: true,
                                mainProduct = value.none { it.accessory || it.sparePart },
                            )
                        newSeries.add(series)
                        if (!dryRun) {
                            LOG.info("creating new series: ${series.title}")
                            seriesRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                                series,
                                isUpdate = false,
                            )
                        }
                        value.map {
                            it.copy(seriesUuid = series.id)
                        }
                    } else {
                        val firstWithSeriesUUID = value.first { it.seriesUuid != null }
                        value.map {
                            it.copy(seriesUuid = firstWithSeriesUUID.seriesUuid)
                        }
                    }
                seriesGroup
            }

        val newProducts = mutableListOf<ProductRegistration>()
        val withProductsId =
            withSeriesId.map {
                if (it.productId == null) {
                        productRegistrationService.findBySupplierRefAndSupplierId(it.supplierRef, it.supplierId)?.let { p ->
                            LOG.info("found product with supplierRef: ${p.supplierRef} and articleName: ${p.articleName}")
                            it.copy(productId = p.id)
                        } ?: run {
                            val product = createNewProduct(it, authentication, dryRun)
                            newProducts.add(product)
                            it.copy(productId = product.id)
                        }
                } else {
                    LOG.info("Found product with productId: ${it.productId} ${it.supplierRef} ${it.accessory} ${it.sparePart}")
                    it
                }
            }

        return ProductAgreementImportResultData(
            productAgreement = withProductsId,
            newSeries = newSeries.distinctBy { it.id },
            newProducts = newProducts.distinctBy { it.supplierRef }
        )
    }

    private suspend fun createNewProduct(
        productAgreement: ProductAgreementRegistrationDTO,
        authentication: Authentication?,
        dryRun: Boolean,
    ): ProductRegistration {
        val product =
            ProductRegistration(
                seriesUUID = productAgreement.seriesUuid!!,
                draftStatus = DraftStatus.DONE,
                adminStatus = if (productAgreement.mainProduct) AdminStatus.PENDING else AdminStatus.APPROVED,
                registrationStatus = RegistrationStatus.ACTIVE,
                articleName = productAgreement.articleName ?: productAgreement.title,
                productData = ProductData(),
                supplierId = productAgreement.supplierId,
                seriesId = productAgreement.seriesUuid.toString(),
                hmsArtNr = productAgreement.hmsArtNr,
                id = UUID.randomUUID(),
                supplierRef = productAgreement.supplierRef,
                accessory = productAgreement.accessory,
                sparePart = productAgreement.sparePart,
                mainProduct = productAgreement.mainProduct,
                createdByUser = authentication?.name ?: "system",
                updatedByUser = authentication?.name ?: "system",
                createdByAdmin = authentication?.isAdmin() ?: true,
            )
        if (!dryRun) {
            LOG.info("creating new product: ${product.articleName}")
            productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(product, isUpdate = false)
        }
        return product
    }

    private fun groupInSeriesBasedOnSupplierRef(
        productagreements: List<ProductAgreementRegistrationDTO>,
    ): Map<String, List<ProductAgreementRegistrationDTO>> = productagreements.groupBy { it.supplierRef }


    data class ProductAgreementImportResultData(
        val productAgreement: List<ProductAgreementRegistrationDTO>,
        val newSeries: List<SeriesRegistration>,
        val newProducts: List<ProductRegistration>,
    )
}

data class ProductAgreementImportResult(
    val insertList: List<ProductAgreementRegistrationDTO>,
    val updateList: List<ProductAgreementRegistrationDTO>,
    val deactivateList: List<ProductAgreementRegistrationDTO>,
    val newSeries: List<SeriesRegistration>,
    val newAccessoryParts: List<ProductRegistration>,
    val newProducts: List<ProductRegistration>,
)
