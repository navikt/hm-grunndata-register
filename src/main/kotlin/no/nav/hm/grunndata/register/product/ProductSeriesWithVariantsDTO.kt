package no.nav.hm.grunndata.register.product

import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import java.time.LocalDateTime
import java.util.UUID

data class ProductSeriesWithVariantsDTO(
    val id: UUID, //trenger vi denne?
    val adminStatus: AdminStatus,
    val created: LocalDateTime,
    val createdBy: String,
    val createdByAdmin: Boolean,
    val createdByUser: String,
    val draftStatus: DraftStatus,
    val expired: LocalDateTime?,
    val isoCategory: String,
    val productData: ProductData,
    val published: LocalDateTime?,
    val registrationStatus: RegistrationStatus,
    val seriesUUID: UUID?,
    val seriesId: String,
    val supplierId: UUID,
    val title: String,
    val updated: LocalDateTime,
    val updatedBy: String,
    val updatedByUser: String,
    val variants: List<ProductRegistrationDTO>
)

fun List<ProductRegistrationDTO>.toProductSeriesWithVariants() =
    minByOrNull { it.created }?.let { mainProduct ->
        val hasVariants = try {
            UUID.fromString(mainProduct.supplierRef)
            true
        } catch (e: IllegalArgumentException) {
            false
        }

        val latestUpdatedProduct = maxBy { it.created }

        ProductSeriesWithVariantsDTO(
            id = mainProduct.id,
            adminStatus = mainProduct.adminStatus,
            registrationStatus = mainProduct.registrationStatus,
            draftStatus = mainProduct.draftStatus,
            created = mainProduct.created,
            createdBy = mainProduct.createdBy,
            createdByAdmin = mainProduct.createdByAdmin,
            createdByUser = mainProduct.createdByUser,
            expired = mainProduct.expired,
            isoCategory = mainProduct.isoCategory,
            productData = mainProduct.productData,
            published = mainProduct.published,
            seriesUUID = mainProduct.seriesUUID,
            seriesId = mainProduct.seriesId,
            supplierId = mainProduct.supplierId,
            title = mainProduct.title,
            updated = latestUpdatedProduct.updated,
            updatedBy = mainProduct.updatedBy,
            updatedByUser = latestUpdatedProduct.updatedByUser,
            variants = if (hasVariants) this else emptyList()
        )
    }