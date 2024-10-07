package no.nav.hm.grunndata.register.series

import jakarta.inject.Singleton
import java.time.LocalDateTime
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.register.HMDB
import no.nav.hm.grunndata.register.iso.IsoCategoryService
import no.nav.hm.grunndata.register.product.ProductDTOMapper
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationService
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService

@Singleton
class SeriesDTOMapper(
    private val productAgreementRegistrationService: ProductAgreementRegistrationService,
    private val productRegistrationService: ProductRegistrationService,
    private val isoCategoryService: IsoCategoryService,
    private val supplierRegistrationService: SupplierRegistrationService,
    private val productDTOMapper: ProductDTOMapper
) {

    suspend fun toDTOV2(seriesRegistration: SeriesRegistration): SeriesRegistrationDTOV2 {
        val supplierName =
            supplierRegistrationService.findById(seriesRegistration.supplierId)?.name
                ?: throw IllegalArgumentException("cannot find series ${seriesRegistration.id} supplier")
        val isoCategoryDTO = isoCategoryService.lookUpCode(seriesRegistration.isoCategory)
            ?: throw IllegalArgumentException("cannot find series ${seriesRegistration.id} isocategory")
        val productRegistrationDTOs = productRegistrationService.findAllBySeriesUuid(seriesRegistration.id)
            .map { product -> productDTOMapper.toDTOV2(product) }
        val inAgreement = productAgreementRegistrationService.findAllByProductIds(
            productRegistrationService.findAllBySeriesUuid(seriesRegistration.id)
                .filter { it.registrationStatus == RegistrationStatus.ACTIVE }
                .map { it.id },
        ).isNotEmpty()

        return SeriesRegistrationDTOV2(
            id = seriesRegistration.id,
            supplierName = supplierName,
            title = seriesRegistration.title,
            text = seriesRegistration.text,
            isoCategory = isoCategoryDTO,
            message = seriesRegistration.message,
            status = EditStatus.from(seriesRegistration),
            seriesData = seriesRegistration.seriesData,
            created = seriesRegistration.created,
            updated = seriesRegistration.updated,
            published = seriesRegistration.published,
            expired = seriesRegistration.expired,
            updatedByUser = seriesRegistration.updatedByUser,
            createdByUser = seriesRegistration.createdByUser,
            variants = productRegistrationDTOs,
            version = seriesRegistration.version,
            isExpired = seriesRegistration.expired < LocalDateTime.now(),
            isPublished = seriesRegistration.published?.let { it < LocalDateTime.now() } ?: false,
            inAgreement = inAgreement,
            hmdbId = if (seriesRegistration.identifier != seriesRegistration.id.toString() &&
                seriesRegistration.updatedBy == HMDB
            ) {
                seriesRegistration.identifier
            } else {
                null
            },
        )
    }

    suspend fun toDTOV2(seriesRegistrationDTO: SeriesRegistrationDTO): SeriesRegistrationDTOV2 {
        val supplierName = supplierRegistrationService.findById(seriesRegistrationDTO.supplierId)?.name
            ?: throw IllegalArgumentException("cannot find series ${seriesRegistrationDTO.id} supplier")
        val isoCategoryDTO = isoCategoryService.lookUpCode(seriesRegistrationDTO.isoCategory)
            ?: throw IllegalArgumentException("cannot find series ${seriesRegistrationDTO.id} isocategory")
        val productRegistrationDTOs = productRegistrationService.findAllBySeriesUuid(seriesRegistrationDTO.id)
            .map { product -> productDTOMapper.toDTOV2(product) }
        val inAgreement = productAgreementRegistrationService.findAllByProductIds(
            productRegistrationService.findAllBySeriesUuid(seriesRegistrationDTO.id)
                .filter { it.registrationStatus == RegistrationStatus.ACTIVE }
                .map { it.id },
        ).isNotEmpty()

        return SeriesRegistrationDTOV2(
            id = seriesRegistrationDTO.id,
            supplierName = supplierName,
            title = seriesRegistrationDTO.title,
            text = seriesRegistrationDTO.text,
            isoCategory = isoCategoryDTO,
            message = seriesRegistrationDTO.message,
            status = EditStatus.from(seriesRegistrationDTO),
            seriesData = seriesRegistrationDTO.seriesData,
            created = seriesRegistrationDTO.created,
            updated = seriesRegistrationDTO.updated,
            published = seriesRegistrationDTO.published,
            expired = seriesRegistrationDTO.expired,
            updatedByUser = seriesRegistrationDTO.updatedByUser,
            createdByUser = seriesRegistrationDTO.createdByUser,
            variants = productRegistrationDTOs,
            version = seriesRegistrationDTO.version,
            isExpired = seriesRegistrationDTO.expired < LocalDateTime.now(),
            isPublished = seriesRegistrationDTO.published?.let { it < LocalDateTime.now() } ?: false,
            inAgreement = inAgreement,
            hmdbId = if (seriesRegistrationDTO.identifier != seriesRegistrationDTO.id.toString() &&
                seriesRegistrationDTO.updatedBy == HMDB
            ) {
                seriesRegistrationDTO.identifier
            } else {
                null
            },
        )
    }
}