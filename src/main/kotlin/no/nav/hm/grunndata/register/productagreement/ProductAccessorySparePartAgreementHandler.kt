package no.nav.hm.grunndata.register.productagreement

import jakarta.inject.Singleton
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.CompatibleWith
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.product.ProductData
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import no.nav.hm.grunndata.register.series.SeriesDataDTO
import no.nav.hm.grunndata.register.series.SeriesRegistration
import no.nav.hm.grunndata.register.series.SeriesRegistrationRepository
import org.slf4j.LoggerFactory

@Singleton
class ProductAccessorySparePartAgreementHandler(
    private val productRegistrationRepository: ProductRegistrationRepository,
    private val seriesRegistrationRepository: SeriesRegistrationRepository
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
    suspend fun handleProductsInProductAgreement(
        productAgreements: List<ProductAgreementRegistrationDTO>,
        dryRun: Boolean = true
    ): ProductAgreementImportResult {
        val distinctProductAgreements = productAgreements.distinctBy { it.supplierRef}
        val mainProductAgreements = distinctProductAgreements.filter { !it.accessory && !it.sparePart }
        val accessoryOrSpareParts = distinctProductAgreements.filter { it.accessory || it.sparePart }
        val supplierId = accessoryOrSpareParts.first().supplierId
        val groupedAccessoryOrSpareParts = groupInSeriesBasedOnTitle(accessoryOrSpareParts)
        val groupedMainProducts = groupInSeriesBasedOnTitle(mainProductAgreements)
        val createdSeriesAccessorSpareParts = createSeriesAndProductsIfNotExists(groupedAccessoryOrSpareParts, supplierId, dryRun)
        val createdSeriesMainProducts = createSeriesAndProductsIfNotExists(groupedMainProducts, supplierId, dryRun)
        val compatibleAccessory = createCompatibleWithLinkForAccessoryParts(createdSeriesAccessorSpareParts, createdSeriesMainProducts, dryRun)
        return ProductAgreementImportResult(
            productAgreements = createdSeriesAccessorSpareParts.productAgreement + createdSeriesMainProducts.productAgreement,
            newSeries = createdSeriesMainProducts.newSeries + createdSeriesAccessorSpareParts.newSeries,
            newAccessoryParts = compatibleAccessory.newProducts,
            newProducts =  createdSeriesMainProducts.newProducts
        )
    }

    private suspend fun createCompatibleWithLinkForAccessoryParts(
        accessorSparePartsResult: ProductAgreementImportResultData,
        mainProductsResult: ProductAgreementImportResultData, dryRun: Boolean
    ): ProductAgreementImportResultData {
        val accessorSpareParts = accessorSparePartsResult.productAgreement
        val mainProducts = mainProductsResult.productAgreement
        if (accessorSpareParts.isEmpty() || mainProducts.isEmpty()) {
            return accessorSparePartsResult
        }
        val compatibleFoundList = mutableListOf<ProductRegistration>()
        // Try to find the main product for each accessory and spare part based on the title similarity
        accessorSpareParts.forEach { accessoryOrSparePart ->
            var mostCompatibleMainProduct: ProductAgreementRegistrationDTO? = null
            var maxWord=0
            mainProducts.forEach { mainProduct ->
                // find intersect size of the two titles
                val size = findTitleIntersectSize(accessoryOrSparePart.title, mainProduct.title)
                if (size>=2 && size>maxWord) {
                    maxWord = size
                    mostCompatibleMainProduct = mainProduct
                }
            }
            if (mostCompatibleMainProduct != null) {
                val product = productRegistrationRepository.findById(accessoryOrSparePart.productId!!)?.let {
                    it.copy(
                        productData = it.productData.copy(attributes = it.productData.attributes.copy(
                            compatibleWidth = CompatibleWith(seriesIds = setOf(mostCompatibleMainProduct!!.seriesUuid!!),
                                productIds = setOf(mostCompatibleMainProduct!!.productId!!))
                        ))
                    )
                } ?: run {
                    accessorSparePartsResult.newProducts.find { it.id == accessoryOrSparePart.productId}?.let {
                        it.copy(
                            productData = it.productData.copy(attributes = it.productData.attributes.copy(
                                compatibleWidth = CompatibleWith(seriesIds = setOf(mostCompatibleMainProduct!!.seriesUuid!!),
                                    productIds = setOf(mostCompatibleMainProduct!!.productId!!))
                            ))
                        )
                    }
                }
                if (product!=null) {
                    compatibleFoundList.add(product)
                    if (!dryRun) productRegistrationRepository.update(product)
                }
            }
        }
        val newProductsWithCompatibleWith = mutableListOf<ProductRegistration>()
        newProductsWithCompatibleWith.addAll(compatibleFoundList)
        newProductsWithCompatibleWith.addAll(accessorSparePartsResult.newProducts.filter { !compatibleFoundList.any { c -> c.id == it.id } })
        return ProductAgreementImportResultData(
            productAgreement = accessorSpareParts,
            newSeries = accessorSparePartsResult.newSeries,
            newProducts = newProductsWithCompatibleWith
        )
    }

    private suspend fun createSeriesAndProductsIfNotExists(
        groupedProductAgreements: Map<String, List<ProductAgreementRegistrationDTO>>,
        supplierId: UUID,
        dryRun: Boolean
    ): ProductAgreementImportResultData {
        val newSeries = mutableListOf<SeriesRegistration>()
        val withSeriesId = groupedProductAgreements.flatMap { (_, value) ->
            // check if in value list that all seriesUuid is null
            val noSeries = value.all { it.seriesUuid == null }
            val seriesGroup = if (noSeries) {
                // create a new series for this group
                val seriesId = UUID.randomUUID()
                val first = value.first()
                val series = SeriesRegistration(
                    id = seriesId,
                    draftStatus = DraftStatus.DONE,
                    adminStatus = AdminStatus.PENDING,
                    supplierId = supplierId,
                    title = first.title,
                    identifier = seriesId.toString(),
                    isoCategory = value.first().isoCategory ?: "0",
                    status = SeriesStatus.ACTIVE,
                    seriesData = SeriesDataDTO(),
                    text = first.title
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
        val withProductsId = distinct.map {
            if (it.productId == null) {
                val product = createNewProduct(it, dryRun)
                newProducts.add(product)
                it.copy(productId = product.id)
            } else {
                it
            }
        }

        return ProductAgreementImportResultData(
            productAgreement = withProductsId,
            newSeries = newSeries,
            newProducts = newProducts
        )
    }

    private suspend fun createNewProduct(
        productAgreement: ProductAgreementRegistrationDTO,
        dryRun: Boolean
    ): ProductRegistration {
        LOG.info("Creating new product for productAgreement: ${productAgreement.supplierRef}")
        val product = ProductRegistration(
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
        )
        if (!dryRun) productRegistrationRepository.save(product)
        return product
    }

    private fun groupInSeriesBasedOnTitle(productsagreements: List<ProductAgreementRegistrationDTO>): MutableMap<String, MutableList<ProductAgreementRegistrationDTO>> {
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
        return groupedSeries
    }

    private fun findCommonPrefix(str1: String, str2: String): String {
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

    fun findTitleIntersectSize(accessoryTitle: String, mainProductTitle: String): Int {
        val accessoryWords = accessoryTitle.split("\\s+".toRegex()).toSet()
        val mainProductWords = mainProductTitle.split("\\s+".toRegex()).toSet()
        val intersection = accessoryWords.intersect(mainProductWords)
        return intersection.size
    }

    data class ProductAgreementImportResultData(
        val productAgreement: List<ProductAgreementRegistrationDTO>,
        val newSeries: List<SeriesRegistration>,
        val newProducts: List<ProductRegistration>
    )
}

data class ProductAgreementImportResult(
    val productAgreements: List<ProductAgreementRegistrationDTO>,
    val newSeries: List<SeriesRegistration>,
    val newAccessoryParts: List<ProductRegistration>,
    val newProducts: List<ProductRegistration>
)