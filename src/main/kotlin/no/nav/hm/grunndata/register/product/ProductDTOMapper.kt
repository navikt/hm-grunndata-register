package no.nav.hm.grunndata.register.product

import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.AgreementInfo
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationService
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistration
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationRepository
import no.nav.hm.grunndata.register.techlabel.TechLabelDTO
import no.nav.hm.grunndata.register.techlabel.TechLabelService
import java.time.LocalDateTime

@Singleton
class ProductDTOMapper(
    private val productAgreementRegistrationRepository: ProductAgreementRegistrationRepository,
    private val techLabelService: TechLabelService,
    private val agreementRegistrationService: AgreementRegistrationService,
    )
{
    suspend fun toDTO(productRegistration: ProductRegistration): ProductRegistrationDTO {
        // TODO cache agreements
        val agreements = productAgreementRegistrationRepository.findBySupplierIdAndSupplierRef(productRegistration.supplierId, productRegistration.supplierRef)
        return ProductRegistrationDTO(
            id = productRegistration.id,
            supplierId = productRegistration.supplierId,
            seriesId = productRegistration.seriesId,
            seriesUUID = productRegistration.seriesUUID,
            supplierRef = productRegistration.supplierRef,
            hmsArtNr = if (!productRegistration.hmsArtNr.isNullOrBlank()) productRegistration.hmsArtNr else if (agreements.isNotEmpty()) agreements.first().hmsArtNr else null,
            title = productRegistration.title,
            articleName = productRegistration.articleName,
            draftStatus = productRegistration.draftStatus,
            adminStatus = productRegistration.adminStatus,
            registrationStatus = productRegistration.registrationStatus,
            message = productRegistration.message,
            adminInfo = productRegistration.adminInfo,
            created = productRegistration.created,
            updated = productRegistration.updated,
            published = productRegistration.published,
            expired = productRegistration.expired,
            updatedByUser = productRegistration.updatedByUser,
            createdByUser = productRegistration.createdByUser,
            createdBy = productRegistration.createdBy,
            updatedBy = productRegistration.updatedBy,
            createdByAdmin = productRegistration.createdByAdmin,
            productData = productRegistration.productData,
            sparePart = productRegistration.sparePart,
            accessory = productRegistration.accessory,
            isoCategory = productRegistration.isoCategory,
            agreements = agreements.map { it.toAgreementInfo() },
            version = productRegistration.version,
        )
    }

    suspend fun toDTOV2(productRegistration: ProductRegistration): ProductRegistrationDTOV2 {
        val agreements = productAgreementRegistrationRepository.findBySupplierIdAndSupplierRef(productRegistration.supplierId, productRegistration.supplierRef)
        val techLabels = techLabelService.fetchLabelsByIsoCode(productRegistration.isoCategory).associateBy { it.label }

        return ProductRegistrationDTOV2(
            id = productRegistration.id,
            seriesUUID = productRegistration.seriesUUID,
            supplierRef = productRegistration.supplierRef,
            hmsArtNr = if (!productRegistration.hmsArtNr.isNullOrBlank()) productRegistration.hmsArtNr else if (agreements.isNotEmpty()) agreements.first().hmsArtNr else null,
            articleName = productRegistration.articleName,
            productData = productRegistration.productData.toProductDataDTO(techLabels),
            sparePart = productRegistration.sparePart,
            created = productRegistration.created,
            accessory = productRegistration.accessory,
            agreements = agreements.map { it.toAgreementInfo() },
            version = productRegistration.version,
            isExpired = productRegistration.expired?.let { it < LocalDateTime.now() } ?: false,
            isPublished = productRegistration.published?.let { it < LocalDateTime.now() } ?: false,
        )
    }

    private fun ProductData.toProductDataDTO(techLabels: Map<String, TechLabelDTO>): ProductDataDTO {
        val techdataDTOs = techLabels.map { (labelKey, techLabel) ->
            techData.find { it.key == labelKey }?.toDTO(techLabels) ?: TechDataDTO(
                key = labelKey,
                value = "",
                unit = techLabel.unit ?: "",
                type = TechDataType.from(techLabel),
                definition = techLabel.definition,
                options = techLabel.options
            )
        }.sortedBy { it.key }

        return ProductDataDTO(
            attributes = attributes,
            techData = techdataDTOs,
        )
    }

    private suspend fun ProductAgreementRegistration.toAgreementInfo(): AgreementInfo {
        val agreement =
            agreementRegistrationService.findById(agreementId)
                ?: throw RuntimeException("Agreement not found") // consider caching agreements
        val delKontrakt = if (postId != null) agreement.delkontraktList.find { postId == it.id } else null
        return AgreementInfo(
            id = agreementId,
            title = agreement.title,
            identifier = agreement.agreementData.identifier,
            rank = rank,
            postNr = post,
            postIdentifier = delKontrakt?.identifier,
            postId = postId,
            refNr = delKontrakt?.delkontraktData?.refNr,
            published = published,
            expired = expired,
            reference = reference,
            postTitle = delKontrakt?.delkontraktData?.title ?: "",
            status = status,
        )
    }
}