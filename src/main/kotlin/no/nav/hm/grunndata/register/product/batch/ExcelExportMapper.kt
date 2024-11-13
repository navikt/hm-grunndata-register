package no.nav.hm.grunndata.register.product.batch

import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.product.ProductRegistrationDTO
import no.nav.hm.grunndata.register.series.SeriesRegistrationRepository

@Singleton
class ExcelExportMapper(
    private val seriesRegistrationRepository: SeriesRegistrationRepository
) {
    suspend fun mapToExportDtos(products: List<ProductRegistrationDTO>): List<ProductExcelExportDto> {
        return products.groupBy { it.seriesUUID }.map {
            val series = seriesRegistrationRepository.findById(it.key)
                ?: throw BadRequestException("No series ${it.key} for products in excel export")
            ProductExcelExportDto(
                products = it.value,
                isoCategory = series.isoCategory,
                seriesUuid = it.key.toString(),
                seriesTitle = series.title,
                seriesDescription = series.text
            )
        }
    }
}

data class ProductExcelExportDto(
    val products: List<ProductRegistrationDTO>,
    val isoCategory: String,
    val seriesUuid: String,
    val seriesTitle: String?,
    val seriesDescription: String?
)