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

    suspend fun handleAccessoryAndSparePartProductAgreement(productAgreements: List<ProductAgreementRegistrationDTO>) {
        val accessoryOrSpareParts = productAgreements.filter { it.accessory || it.sparePart }
        val supplierId = accessoryOrSpareParts.first().supplierId
        val groupedAccessoryOrSpareParts = groupInSeriesBasedOnTitle(accessoryOrSpareParts)
        val accessoryOrSparePartsWithSeries = groupedAccessoryOrSpareParts.flatMap { (_, value) ->
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
    }

    private fun groupInSeriesBasedOnTitle(accessoryOrSpareParts: List<ProductAgreementRegistrationDTO>): MutableMap<UUID, MutableList<ProductAgreementRegistrationDTO>> {
        // group accessory and spare parts together in series based on the title that has intersection size >= 2
        val groupedSeries = mutableMapOf<UUID, MutableList<ProductAgreementRegistrationDTO>>()
        val visited = mutableSetOf<UUID>()
        accessoryOrSpareParts.forEach { accessoryOrSparePart ->
            if (visited.contains(accessoryOrSparePart.id)) {
                return@forEach
            }
            val currentSeries = mutableListOf(accessoryOrSparePart)
            val titleWords = accessoryOrSparePart.title.split("\\s+".toRegex()).toSet()
            accessoryOrSpareParts.filter { it.id != accessoryOrSparePart.id && it.id !in visited}.forEach {
                val otherTitleWords = it.title.split("\\s+".toRegex()).toSet()
                if (titleWords.intersect(otherTitleWords).size >= 2) {
                    currentSeries.add(it)
                    visited.add(it.id)
                }
            }
            groupedSeries[accessoryOrSparePart.id] = currentSeries
            visited.add(accessoryOrSparePart.id)
        }
        return groupedSeries
    }
}