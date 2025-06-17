package no.nav.hm.grunndata.register.part

import io.micronaut.security.authentication.Authentication
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.product.ProductData
import no.nav.hm.grunndata.register.product.ProductDataDTO
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.isAdmin
import no.nav.hm.grunndata.register.product.isHms
import no.nav.hm.grunndata.register.series.SeriesDataDTO
import no.nav.hm.grunndata.register.series.SeriesRegistration
import java.time.LocalDateTime
import java.util.UUID

data class PartDTO(
    val id: UUID,
    val seriesUUID: UUID?,
    val hmsArtNr: String?,
    val supplierRef: String,
    val supplierName: String,
    val articleName: String,
    val accessory: Boolean,
    val sparePart: Boolean,
    val productData: ProductDataDTO,
    val isPublished: Boolean,
    val isExpired: Boolean
)

data class PartDraftWithDTO(
    val title: String,
    val isoCategory: String,
    val hmsArtNr: String? = null,
    val levArtNr: String,
    val sparePart: Boolean? = false,
    val accessory: Boolean? = false,
    val supplierId: UUID,

    ) {
    init {
        if (title.isBlank()) throw BadRequestException("title is required")
        if (isoCategory.isBlank()) throw BadRequestException("isoCategory is required")
        if (levArtNr.isBlank()) throw BadRequestException("levArtNr is required")
        if ((sparePart == null && accessory == null) || (sparePart == false && accessory == false)) throw BadRequestException(
            "sparePart or accessory is required"
        )

    }
}


fun PartDraftWithDTO.toSeriesRegistration(authentication: Authentication): SeriesRegistration {
    val seriesId = UUID.randomUUID()
    return SeriesRegistration(
        id = seriesId,
        supplierId = supplierId,
        title = title,
        isoCategory = isoCategory,
        mainProduct = false,
        text = "",
        identifier = seriesId.toString(),
        draftStatus = DraftStatus.DRAFT,
        adminStatus = AdminStatus.PENDING,
        status = SeriesStatus.ACTIVE,
        createdBy = REGISTER,
        updatedBy = REGISTER,
        createdByUser = authentication.name,
        updatedByUser = authentication.name,
        createdByAdmin = authentication.isAdmin() || authentication.isHms(),
        created = LocalDateTime.now(),
        updated = LocalDateTime.now(),
        seriesData = SeriesDataDTO(media = emptySet()),
        version = 0,
    )
}


fun PartDraftWithDTO.toProductRegistration(seriesUUID: UUID, authentication: Authentication): ProductRegistration {
    val productId = UUID.randomUUID()
    return ProductRegistration(
        id = productId,
        seriesId = seriesUUID.toString(),
        seriesUUID = seriesUUID,
        supplierId = supplierId,
        supplierRef = levArtNr,
        hmsArtNr = hmsArtNr,
        draftStatus = DraftStatus.DRAFT,
        registrationStatus = RegistrationStatus.ACTIVE,
        adminStatus = AdminStatus.PENDING,
        title = title,
        articleName = title,
        isoCategory = isoCategory,
        sparePart = sparePart ?: false,
        accessory = accessory ?: false,
        mainProduct = false,
        expired = LocalDateTime.now().plusYears(3),
        productData =
            ProductData(),
        createdByAdmin = authentication.isAdmin() || authentication.isHms(),
    )
}

data class UpdatePartDto(
    val hmsArtNr: String? = null,
    val supplierRef: String? = null,
    val title: String? = null,
)
