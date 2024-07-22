package no.nav.hm.grunndata.register.productagreement

import jakarta.inject.Singleton
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.AdminStatus
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
     * This function will handle the products, accessory and spareparts in the productAgreement.
     * It will group the products agreements in series based on the title
     * create the series and the products if not exists and dryRun is false
     * returns a list of productAgreements with seriesUuid and productId
     * The products will have admin status PENDING and draft status DONE
     */
    suspend fun handleProductsInProductAgreement(
        productAgreements: List<ProductAgreementRegistrationDTO>,
        dryRun: Boolean = true
    ): List<ProductAgreementRegistrationDTO> {
        val mainProductAgreements = productAgreements.filter { !it.accessory && !it.sparePart }
        val accessoryOrSpareParts = productAgreements.filter { it.accessory || it.sparePart }
        val supplierId = accessoryOrSpareParts.first().supplierId
        val groupedAccessoryOrSpareParts = groupInSeriesBasedOnTitle(accessoryOrSpareParts)
        val groupedMainProducts = groupInSeriesBasedOnTitle(mainProductAgreements)
        val createdSeriesAccessorSpareParts = createSeriesAndProducts(groupedAccessoryOrSpareParts, supplierId, dryRun)
        val createdSeriesMainProducts = createSeriesAndProducts(groupedMainProducts, supplierId, dryRun)
        return createdSeriesAccessorSpareParts + createdSeriesMainProducts
    }

    private suspend fun createSeriesAndProducts(
        groupedProductAgreements: Map<String, List<ProductAgreementRegistrationDTO>>,
        supplierId: UUID,
        dryRun: Boolean
    ): List<ProductAgreementRegistrationDTO> {
        val createdSeries = groupedProductAgreements.flatMap { (_, value) ->
            // check if in value list that all seriesUuid is null
            val noSeries = value.all { it.seriesUuid == null }
            val seriesGroup = if (noSeries) {
                // create a new series for this group of accessory and spareParts
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
        val createdSeriesAndProducts = createdSeries.map {
            if (it.productId == null) {
                val product = createNewProduct(it, dryRun)
                it.copy(productId = product.id)
            } else {
                it
            }
        }

        return createdSeries
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
            if (visited.contains(productAgreement.id)) {
                return@forEach
            }
            visited.add(productAgreement.id)
            var isGrouped = false
            orderedProductAgreements.filter { it.id != productAgreement.id && it.id !in visited }
                .forEach { otherProductAgreement ->
                    val commonPrefixTitle = findCommonPrefix(productAgreement.title, otherProductAgreement.title)
                    if (commonPrefixTitle.split("\\s+".toRegex()).size >= 2) {
                        if (!groupedSeries.containsKey(commonPrefixTitle)) {
                            groupedSeries[commonPrefixTitle] = mutableListOf(productAgreement)
                        }
                        groupedSeries[commonPrefixTitle]?.add(otherProductAgreement)
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
}