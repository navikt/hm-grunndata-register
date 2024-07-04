package no.nav.hm.grunndata.register.productagreement

import jakarta.inject.Singleton
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import no.nav.hm.grunndata.register.series.SeriesDataDTO
import no.nav.hm.grunndata.register.series.SeriesRegistration
import no.nav.hm.grunndata.register.series.SeriesRegistrationRepository

@Singleton
class ProductAgreementAccessoryPartHandler(private val productRegistrationRepository: ProductRegistrationRepository,
                                           private val seriesRegistrationRepository: SeriesRegistrationRepository) {

    suspend fun handleAccessoryAndSparePartProductAgreement(productAgreements: List<ProductAgreementRegistrationDTO>): List<ProductAgreementRegistrationDTO> {
        val mainProductAgreements = productAgreements.filter { !it.accessory && !it.sparePart }
        val accessoryOrSpareParts = productAgreements.filter { it.accessory || it.sparePart }
        val supplierId = accessoryOrSpareParts.first().supplierId
        val groupedAccessoryOrSpareParts = groupInSeriesBasedOnTitle(accessoryOrSpareParts)
        val groupedMainProducts = groupInSeriesBasedOnTitle(mainProductAgreements)
        val createdSeriesAccessorSpareParts = createSeriesAndProducts(groupedAccessoryOrSpareParts, supplierId)
        val createdSeriesMainProducts = createSeriesAndProducts(groupedMainProducts, supplierId)

        return createdSeriesAccessorSpareParts + createdSeriesMainProducts
    }

    private suspend fun createSeriesAndProducts(groupedProductAgreements: Map<String, List<ProductAgreementRegistrationDTO>>, supplierId: UUID): List<ProductAgreementRegistrationDTO> {
        val createdSeries = groupedProductAgreements.flatMap { (_, value) ->
            // check if in value list that all seriesUuid is null
            val noSeries = value.all { it.seriesUuid == null }
            val seriesGroup = if (noSeries) {
                // create a new series for this group of accessory and spareParts
                val seriesId = UUID.randomUUID()
                val first =  value.first()
                val series = seriesRegistrationRepository.save(SeriesRegistration(
                    id = seriesId,
                    supplierId = supplierId,
                    title = first.title,
                    identifier = seriesId.toString(),
                    isoCategory = value.first().isoCategory ?: "0",
                    status = SeriesStatus.ACTIVE,
                    seriesData = SeriesDataDTO(),
                    text = first.title
                ))
                value.map {
                    it.copy(seriesUuid = series.id)
                }
            }
            else {
                val firstWithSeriesUUID = value.first { it.seriesUuid != null }
                value.map {
                    it.copy(seriesUuid = firstWithSeriesUUID.seriesUuid)
                }
            }
            seriesGroup
        }
        return createdSeries
    }

    private fun groupInSeriesBasedOnTitle(productsagreements: List<ProductAgreementRegistrationDTO>): MutableMap<String, MutableList<ProductAgreementRegistrationDTO>> {
        // group accessory and spare parts together in series based on the title that has intersection size >= 2
        val groupedSeries = mutableMapOf<String, MutableList<ProductAgreementRegistrationDTO>>()
        val visited = mutableSetOf<UUID>()
        productsagreements.forEach { productAgreement ->
            if (visited.contains(productAgreement.id)) {
                return@forEach
            }
            visited.add(productAgreement.id)
            var isGrouped = false
            productsagreements.filter { it.id != productAgreement.id && it.id !in visited}.forEach { otherProductAgreement ->
                val titleWords = productAgreement.title.split("\\s+".toRegex()).toSet()
                val otherTitleWords = otherProductAgreement.title.split("\\s+".toRegex()).toSet()
                val intersection = titleWords.intersect(otherTitleWords)
                if (intersection.size >= 2) {
                    val intersectedTitle = intersection.joinToString ( " " )
                    if (!groupedSeries.containsKey(intersectedTitle)) {
                        groupedSeries[intersectedTitle] = mutableListOf(productAgreement)
                    }
                    groupedSeries[intersectedTitle]?.add(otherProductAgreement)
                    visited.add(otherProductAgreement.id)
                    isGrouped= true
                }
            }
            if (!isGrouped) {
                groupedSeries[productAgreement.title] = mutableListOf(productAgreement)
            }
        }
        return groupedSeries
    }
}