package no.nav.hm.grunndata.register.productagreement

import io.micronaut.security.authentication.Authentication
import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.CompatibleWith
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.product.ProductData
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import no.nav.hm.grunndata.register.product.isAdmin
import no.nav.hm.grunndata.register.series.SeriesDataDTO
import no.nav.hm.grunndata.register.series.SeriesRegistration
import no.nav.hm.grunndata.register.series.SeriesRegistrationRepository
import org.slf4j.LoggerFactory
import java.util.UUID

@Singleton
class ProductAccessorySparePartAgreementHandler(
    private val productRegistrationRepository: ProductRegistrationRepository,
    private val seriesRegistrationRepository: SeriesRegistrationRepository,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(ProductAccessorySparePartAgreementHandler::class.java)
    }

    /**
     * This function will handle the products, accessory and spareparts in the productAgreement catalog.
     * It will group the products agreements in series based on the title
     * create the series and the products if not exists and dryRun is false
     * returns a list of productAgreements with seriesUuid and productId
     * The products will have admin status PENDING and draft status DONE
     */
    suspend fun handleNewProductsInExcelImport(
        importResult: ProductAgreementRegistrationResult,
        authentication: Authentication?,
        dryRun: Boolean = true,
    ): ProductAgreementImportResult {
        val newProductAgreements = importResult.insertedList
        val distinctProductAgreements = newProductAgreements.distinctBy { it.supplierRef }
        val mainProductAgreements = distinctProductAgreements.filter { !it.accessory && !it.sparePart }
        val accessoryOrSpareParts = distinctProductAgreements.filter { it.accessory || it.sparePart }
        val supplierId = accessoryOrSpareParts.first().supplierId
        val groupedAccessoryOrSpareParts = groupInSeriesBasedOnTitle(accessoryOrSpareParts)
        val groupedMainProducts = groupInSeriesBasedOnTitle(mainProductAgreements)
        val createdAccessorSpareParts =
            createSeriesAndProductsIfNotExists(groupedAccessoryOrSpareParts, supplierId, authentication, dryRun)
        val createdMainProducts =
            createSeriesAndProductsIfNotExists(groupedMainProducts, supplierId, authentication, dryRun)
        val compatibleAccessory =
            createCompatibleWithLinkForAccessoryParts(createdAccessorSpareParts, createdMainProducts, dryRun)
        return ProductAgreementImportResult(
            newProductAgreements = createdAccessorSpareParts.productAgreement + createdMainProducts.productAgreement,
            newSeries = createdMainProducts.newSeries + createdAccessorSpareParts.newSeries,
            newAccessoryParts = compatibleAccessory.newProducts,
            newProducts = createdMainProducts.newProducts,
        )
    }

    private suspend fun createCompatibleWithLinkForAccessoryParts(
        accessorSparePartsResult: ProductAgreementImportResultData,
        mainProductsResult: ProductAgreementImportResultData,
        dryRun: Boolean,
    ): ProductAgreementImportResultData {
        if (accessorSparePartsResult.newProducts.isEmpty() || mainProductsResult.newProducts.isEmpty()) {
            return accessorSparePartsResult
        }
        val accessorSpareParts = accessorSparePartsResult.productAgreement
        val mainProducts = mainProductsResult.productAgreement
        if (accessorSpareParts.isEmpty() || mainProducts.isEmpty()) {
            return accessorSparePartsResult
        }
        val compatibleFoundList = mutableListOf<ProductRegistration>()
        // Try to find the main product for each accessory and spare part based on the title similarity
        accessorSpareParts.forEach { accessoryOrSparePart ->
            var mostCompatibleMainProduct: ProductAgreementRegistrationDTO? = null
            var maxWord = 0
            mainProducts.forEach { mainProduct ->
                // find intersect size of the two titles
                val size = findTitleIntersectSize(accessoryOrSparePart.title, mainProduct.title)
                if (size >= 2 && size > maxWord) {
                    maxWord = size
                    mostCompatibleMainProduct = mainProduct
                }
            }
            if (mostCompatibleMainProduct != null) {
                val product =
                    productRegistrationRepository.findById(accessoryOrSparePart.productId!!)?.let {
                        it.copy(
                            productData =
                                it.productData.copy(
                                    attributes =
                                        it.productData.attributes.copy(
                                            compatibleWidth =
                                                CompatibleWith(
                                                    seriesIds = setOf(mostCompatibleMainProduct!!.seriesUuid!!),
                                                    productIds = setOf(mostCompatibleMainProduct!!.productId!!),
                                                ),
                                        ),
                                ),
                        )
                    } ?: run {
                        accessorSparePartsResult.newProducts.find { it.id == accessoryOrSparePart.productId }?.let {
                            it.copy(
                                productData =
                                    it.productData.copy(
                                        attributes =
                                            it.productData.attributes.copy(
                                                compatibleWidth =
                                                    CompatibleWith(
                                                        seriesIds = setOf(mostCompatibleMainProduct!!.seriesUuid!!),
                                                        productIds = setOf(mostCompatibleMainProduct!!.productId!!),
                                                    ),
                                            ),
                                    ),
                            )
                        }
                    }
                if (product != null) {
                    compatibleFoundList.add(product)
                    if (!dryRun) productRegistrationRepository.update(product)
                }
            }
        }
        val newProductsWithCompatibleWith = mutableListOf<ProductRegistration>()
        newProductsWithCompatibleWith.addAll(compatibleFoundList)
        newProductsWithCompatibleWith.addAll(
            accessorSparePartsResult.newProducts.filter {
                !compatibleFoundList.any {
                        c ->
                    c.id == it.id
                }
            },
        )
        return ProductAgreementImportResultData(
            productAgreement = accessorSpareParts,
            newSeries = accessorSparePartsResult.newSeries,
            newProducts = newProductsWithCompatibleWith,
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
                val seriesGroup =
                    if (noSeries) {
                        // create a new series for this group
                        val seriesId = UUID.randomUUID()
                        val series =
                            SeriesRegistration(
                                id = seriesId,
                                draftStatus = DraftStatus.DONE,
                                adminStatus = AdminStatus.PENDING,
                                supplierId = supplierId,
                                title = key.trim(),
                                identifier = seriesId.toString(),
                                isoCategory = value.first().isoCategory ?: "0",
                                status = SeriesStatus.ACTIVE,
                                seriesData = SeriesDataDTO(),
                                text = key.trim(),
                                createdByUser = authentication?.name ?: "system",
                                updatedByUser = authentication?.name ?: "system",
                                createdByAdmin = authentication?.isAdmin() ?: true,
                                mainProduct = value.none { it.accessory || it.sparePart },
                            )
                        newSeries.add(series)
                        if (!dryRun) seriesRegistrationRepository.save(series)
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
        // only save one product if more than one productAgreement. Because one product can be in many productAgreements
        val distinct = withSeriesId.distinctBy { it.supplierRef }
        val newProducts = mutableListOf<ProductRegistration>()
        val withProductsId =
            distinct.map {
                if (it.productId == null) {
                    val product = createNewProduct(it, authentication, dryRun)
                    newProducts.add(product)
                    it.copy(productId = product.id)
                } else {
                    it
                }
            }

        return ProductAgreementImportResultData(
            productAgreement = withProductsId,
            newSeries = newSeries,
            newProducts = newProducts,
        )
    }

    private suspend fun createNewProduct(
        productAgreement: ProductAgreementRegistrationDTO,
        authentication: Authentication?,
        dryRun: Boolean,
    ): ProductRegistration {
        LOG.info("Creating new product for productAgreement: ${productAgreement.supplierRef}")
        val product =
            ProductRegistration(
                seriesUUID = productAgreement.seriesUuid!!,
                draftStatus = DraftStatus.DONE,
                adminStatus = AdminStatus.PENDING,
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
                createdByUser = authentication?.name ?: "system",
                updatedByUser = authentication?.name ?: "system",
                createdByAdmin = authentication?.isAdmin() ?: true,
            )
        if (!dryRun) productRegistrationRepository.save(product)
        return product
    }

    private fun groupInSeriesBasedOnTitle(
        productsagreements: List<ProductAgreementRegistrationDTO>,
    ): Map<String, MutableList<ProductAgreementRegistrationDTO>> {
        // group accessory and spare parts together in series based on the common prefix of the title
        val orderedProductAgreements = productsagreements.sortedBy { it.title }
        val groupedSeries = mutableMapOf<String, MutableList<ProductAgreementRegistrationDTO>>()
        val visited = mutableSetOf<UUID>()
        orderedProductAgreements.forEach { productAgreement ->
            var isGrouped = false
            if (visited.contains(productAgreement.id)) {
                return@forEach
            }
            visited.add(productAgreement.id)
            orderedProductAgreements.filter { it.id != productAgreement.id && it.id !in visited }
                .forEach { otherProductAgreement ->
                    val commonPrefixTitle = findCommonPrefix(productAgreement.title, otherProductAgreement.title).trim()
                    if (commonPrefixTitle.split("\\s+".toRegex()).size >= 3) {
                        if (!groupedSeries.containsKey(productAgreement.title)) {
                            groupedSeries[productAgreement.title] = mutableListOf()
                            if (!isGrouped) {
                                groupedSeries[productAgreement.title]?.add(productAgreement)
                            }
                        }
                        groupedSeries[productAgreement.title]?.add(otherProductAgreement)
                        visited.add(otherProductAgreement.id)
                        isGrouped = true
                    }
                }
            if (!isGrouped) {
                groupedSeries[productAgreement.title] = mutableListOf(productAgreement)
            }
        }
        return groupedSeries.mapKeys {
            if (it.value.size > 1) {
                findCommonPrefix(
                    it.value[0].title,
                    it.value[1].title,
                )
            } else {
                it.value[0].title
            }
        }
    }

    private fun findCommonPrefix(
        str1: String,
        str2: String,
    ): String {
        val minLength = minOf(str1.length, str2.length)
        val commonPrefix = StringBuilder()

        for (i in 0 until minLength) {
            if (str1[i] == str2[i]) {
                commonPrefix.append(str1[i])
            } else {
                break
            }
        }

        return commonPrefix.toString()
    }

    fun findTitleIntersectSize(
        accessoryTitle: String,
        mainProductTitle: String,
    ): Int {
        val accessoryWords = accessoryTitle.split("\\s+".toRegex()).toSet()
        val mainProductWords = mainProductTitle.split("\\s+".toRegex()).toSet()
        val intersection = accessoryWords.intersect(mainProductWords)
        return intersection.size
    }

    data class ProductAgreementImportResultData(
        val productAgreement: List<ProductAgreementRegistrationDTO>,
        val newSeries: List<SeriesRegistration>,
        val newProducts: List<ProductRegistration>,
    )
}

data class ProductAgreementImportResult(
    val newProductAgreements: List<ProductAgreementRegistrationDTO>,
    val newSeries: List<SeriesRegistration>,
    val newAccessoryParts: List<ProductRegistration>,
    val newProducts: List<ProductRegistration>,
)
