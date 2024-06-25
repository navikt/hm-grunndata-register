package no.nav.hm.grunndata.register.productagreement

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Column
import java.time.LocalDateTime
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.AgreementInfo
import no.nav.hm.grunndata.rapid.dto.ProductAgreementRegistrationRapidDTO
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.rapid.dto.RapidDTO
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.event.EventPayload


@MappedEntity("product_agreement_reg_v1")
data class ProductAgreementRegistration(
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val productId: UUID? = null,
    val seriesUuid: UUID? = null,
    val title: String,
    val articleName: String?,
    val supplierId: UUID,
    val supplierRef: String,
    @field:Column(name = "hms_artnr")
    val hmsArtNr: String?,
    val agreementId: UUID,
    val reference: String,
    val post: Int,
    val rank: Int,
    val postId: UUID?,
    val status: ProductAgreementStatus = ProductAgreementStatus.ACTIVE,
    val createdBy: String,
    val updatedBy: String = REGISTER,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val published: LocalDateTime = LocalDateTime.now(),
    val expired: LocalDateTime = LocalDateTime.now().plusYears(4)
)
data class ProductAgreementRegistrationDTO(
    override val id: UUID = UUID.randomUUID(),
    val productId: UUID?,
    val seriesUuid: UUID?,
    val title: String,
    val articleName: String?,
    val supplierId: UUID,
    val supplierRef: String,
    val hmsArtNr: String?,
    val agreementId: UUID,
    val reference: String,
    val post: Int,
    val rank: Int,
    val postId: UUID?,
    val status: ProductAgreementStatus = ProductAgreementStatus.ACTIVE,
    val createdBy: String = ProductAgreementImportExcelService.EXCEL,
    val updatedBy: String = REGISTER,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val published: LocalDateTime,
    val expired: LocalDateTime,
    override val updatedByUser: String = "system"
) : EventPayload {
    override fun toRapidDTO(): RapidDTO = ProductAgreementRegistrationRapidDTO(
        id = id, productId = productId,
        agreementId = agreementId, post = post, postId = postId,
        rank = rank, hmsArtNr = hmsArtNr, reference = reference, status = status, title = title,
        supplierId = supplierId, supplierRef = supplierRef, created = created, updated = updated,
        published = published, expired = expired, createdBy = createdBy
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
        hmsArtNr = hmsArtNr,
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
        updatedBy = updatedBy
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
        hmsArtNr = hmsArtNr,
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
        updatedBy = updatedBy
    )
}

fun List<ProductAgreementRegistration>.toDTO(): List<ProductAgreementRegistrationDTO> = map { it.toDTO() }

fun ProductAgreementRegistration.toInfo() = AgreementInfo(
    id = agreementId, reference = reference,
    postNr = post, rank = rank, expired = expired, postId = postId, status = status
)

fun ProductAgreementRegistrationDTO.toInfo() = AgreementInfo(
    id = agreementId, reference = reference, postNr = post, rank = rank, expired = expired, status = status
)

