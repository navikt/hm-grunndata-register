package no.nav.hm.grunndata.register.productagreement

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Column
import no.nav.hm.grunndata.rapid.dto.AgreementInfo
import no.nav.hm.grunndata.rapid.dto.ProductAgreementRegistrationRapidDTO
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.rapid.dto.RapidDTO
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.event.EventPayload
import java.time.LocalDateTime
import java.util.UUID

@MappedEntity("product_agreement_reg_v1")
data class ProductAgreementRegistration(
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val productId: UUID,
    @Deprecated("Use productId to join with product table to get seriesUuid")
    val seriesUuid: UUID? = null,
    val title: String,
    val articleName: String?,
    val sparePart: Boolean = false,
    val accessory: Boolean = false,
    val mainProduct: Boolean = true,
    val supplierId: UUID,
    val supplierRef: String,
    @Deprecated("Use productId to join with product table to get hmsArtNr")
    @field:Column(name = "hms_artnr")
    val hmsArtNr: String? = null,
    val agreementId: UUID,
    val reference: String,
    val post: Int,
    val rank: Int,
    val postId: UUID,
    val status: ProductAgreementStatus = ProductAgreementStatus.ACTIVE,
    val createdBy: String,
    val updatedBy: String = REGISTER,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val published: LocalDateTime = LocalDateTime.now(),
    val expired: LocalDateTime = LocalDateTime.now().plusYears(4),
)

data class ProductAgreementRegistrationDTO(
    override val id: UUID = UUID.randomUUID(),
    val productId: UUID,
    @Deprecated("Use productId to join with product table to get seriesUuid")
    val seriesUuid: UUID?,
    val title: String,
    val articleName: String?,
    val accessory: Boolean = false,
    val sparePart: Boolean = false,
    val mainProduct: Boolean = true,
    val isoCategory: String? = null,
    val supplierId: UUID,
    val supplierRef: String,
    val agreementId: UUID,
    val reference: String,
    val post: Int,
    val rank: Int,
    val postId: UUID,
    val status: ProductAgreementStatus = ProductAgreementStatus.INACTIVE,
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val published: LocalDateTime,
    val expired: LocalDateTime,
    override val updatedByUser: String = "system",
) : EventPayload {
    override fun toRapidDTO(): RapidDTO =
        ProductAgreementRegistrationRapidDTO(
            id = id,
            // SeriesUUID, can be null so have to use agreementId
            partitionKey = agreementId.toString(),
            productId = productId,
            agreementId = agreementId,
            post = post,
            postId = postId,
            rank = rank,
            reference = reference,
            status = status,
            title = title,
            articleName = articleName?: title,
            supplierId = supplierId,
            supplierRef = supplierRef,
            created = created,
            updated = updated,
            published = published,
            expired = expired,
            createdBy = createdBy,
            mainProduct = mainProduct,
            sparePart = sparePart,
            accessory = accessory
        )
}

fun ProductAgreementRegistrationDTO.toEntity(): ProductAgreementRegistration {
    return ProductAgreementRegistration(
        id = id,
        productId = productId,
        seriesUuid = seriesUuid,
        title = title,
        articleName = articleName,
        supplierId = supplierId,
        supplierRef = supplierRef,
        agreementId = agreementId,
        reference = reference,
        post = post,
        rank = rank,
        postId = postId,
        status = status,
        createdBy = createdBy,
        created = created,
        updated = updated,
        published = published,
        expired = expired,
        updatedBy = updatedBy,
        sparePart = sparePart,
        accessory = accessory,
        mainProduct = mainProduct
    )
}

fun List<ProductAgreementRegistrationDTO>.toEntity(): List<ProductAgreementRegistration> = map { it.toEntity() }

fun ProductAgreementRegistration.toDTO(): ProductAgreementRegistrationDTO {
    return ProductAgreementRegistrationDTO(
        id = id,
        productId = productId,
        seriesUuid = seriesUuid,
        title = title,
        articleName = articleName,
        supplierId = supplierId,
        supplierRef = supplierRef,
        agreementId = agreementId,
        reference = reference,
        post = post,
        rank = rank,
        postId = postId,
        status = status,
        createdBy = createdBy,
        created = created,
        updated = updated,
        published = published,
        expired = expired,
        updatedBy = updatedBy,
        accessory = accessory,
        sparePart = sparePart,
        mainProduct = mainProduct,
        isoCategory = null,
    )
}

fun List<ProductAgreementRegistration>.toDTO(): List<ProductAgreementRegistrationDTO> = map { it.toDTO() }
