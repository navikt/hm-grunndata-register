package no.nav.hm.grunndata.register.product

import no.nav.helse.rapids_rivers.toUUID
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.Attributes
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.register.product.batch.ProductRegistrationExcelDTO
import java.util.*

class ProductExcelMapper(private val productRegistrationService: ProductRegistrationService) {

    fun mapToProductRegistrationDTO(excelDTO: ProductRegistrationExcelDTO): ProductRegistrationDTO {
        val productId = excelDTO.produktid?.toUUID()?: UUID.randomUUID()
        val seriesUUID = excelDTO.produktserieid?.toUUID() ?: UUID.randomUUID()
        val supplierId = excelDTO.leverandorid.toUUID()
        return ProductRegistrationDTO(
            id = productId,
            seriesId = seriesUUID.toString(),
            seriesUUID = seriesUUID,
            supplierId = supplierId,
            supplierRef = excelDTO.levartnr,
            hmsArtNr = excelDTO.hmsnr,
            draftStatus = DraftStatus.DRAFT,
            registrationStatus = RegistrationStatus.ACTIVE,
            adminStatus = AdminStatus.PENDING,
            title = excelDTO.produktseriesnavn?: excelDTO.produktnavn?: "",
            articleName = excelDTO.produktnavn?: excelDTO.produktseriesnavn?: "",
            isoCategory = excelDTO.isoCategory,
            productData = ProductData(
                attributes = Attributes(
                    shortdescription = excelDTO.andrespesifikasjoner,
                    text = excelDTO.produktseriebeskrivelse
                ),
                techData = excelDTO.techData,
            )
        )
    }
}