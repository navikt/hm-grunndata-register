package no.nav.hm.grunndata.register.series

import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.register.HMDB
import no.nav.hm.grunndata.register.iso.IsoCategoryService
import no.nav.hm.grunndata.register.product.ProductDTOMapper
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationService
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import java.time.LocalDateTime

@Singleton
class SeriesDTOMapper(
    private val productAgreementRegistrationService: ProductAgreementRegistrationService,
    private val productRegistrationService: ProductRegistrationService,
    private val isoCategoryService: IsoCategoryService,
    private val supplierRegistrationService: SupplierRegistrationService,
    private val productDTOMapper: ProductDTOMapper
) {

    suspend fun toDTOV2(seriesRegistration: SeriesRegistration): SeriesDTO {
        val supplierName =
            supplierRegistrationService.findById(seriesRegistration.supplierId)?.name
                ?: throw IllegalArgumentException("cannot find series ${seriesRegistration.id} supplier")
        val isoCategoryDTO = isoCategoryService.lookUpCode(seriesRegistration.isoCategory)
        val productRegistrationDTOs = productRegistrationService.findAllBySeriesUuid(seriesRegistration.id)
            .filter { it.registrationStatus != RegistrationStatus.DELETED }
            .map { product -> productDTOMapper.toDTOV2(product) }
        val inAgreement = productAgreementRegistrationService.findAllByProductIds(
            productRegistrationService.findAllBySeriesUuid(seriesRegistration.id)
                .filter { it.registrationStatus == RegistrationStatus.ACTIVE }
                .map { it.id },
        ).isNotEmpty()

        return SeriesDTO(
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
                seriesRegistration.createdBy == HMDB
            ) {
                seriesRegistration.identifier
            } else {
                null
            },
        )
    }

}