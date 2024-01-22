package no.nav.hm.grunndata.register.productagreement

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Column
import no.nav.hm.grunndata.rapid.dto.AgreementInfo
import no.nav.hm.grunndata.rapid.dto.ProductAgreementRegistrationRapidDTO
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.rapid.dto.RapidDTO
import no.nav.hm.grunndata.register.event.EventPayload
import java.time.LocalDateTime
import java.util.UUID


@MappedEntity("product_agreement_reg_v1")
data class ProductAgreementRegistration(
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val productId: UUID? = null,
    val seriesId: String? = null,
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
    val status: ProductAgreementStatus = ProductAgreementStatus.ACTIVE,
    val createdBy: String,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val published: LocalDateTime = LocalDateTime.now(),
    val expired: LocalDateTime = LocalDateTime.now().plusYears(4)
)

data class ProductAgreementRegistrationDTO(
    override val id: UUID = UUID.randomUUID(),
    val productId: UUID?,
    val seriesId: String?,
    val title: String,
    val articleName: String?,
    val supplierId: UUID,
    val supplierRef: String,
    val hmsArtNr: String?,
    val agreementId: UUID,
    val reference: String,
    val post: Int,
    val rank: Int,
    val status: ProductAgreementStatus = ProductAgreementStatus.ACTIVE,
    val createdBy: String = ProductAgreementImportExcelService.EXCEL,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val published: LocalDateTime,
    val expired: LocalDateTime,
    override val updatedByUser: String = "system",
) : EventPayload {
    override fun toRapidDTO(): RapidDTO = ProductAgreementRegistrationRapidDTO(
        id = id, productId = productId,
        agreementId = agreementId, post = post,
        rank = rank, hmsArtNr = hmsArtNr, reference = reference, status = status, title = title,
        supplierId = supplierId, supplierRef = supplierRef, created = created, updated = updated,
        published = published, expired = expired, createdBy = createdBy
    )

}


fun ProductAgreementRegistrationDTO.toEntity(): ProductAgreementRegistration {
    return ProductAgreementRegistration(
        id = id,
        productId = productId,
        seriesId = seriesId,
        title = title,
        articleName = articleName,
        supplierId = supplierId,
        supplierRef = supplierRef,
        hmsArtNr = hmsArtNr,
        agreementId = agreementId,
        reference = reference,
        post = post,
        rank = rank,
        status = status,
        createdBy = createdBy,
        created = created,
        updated = updated,
        published = published,
        expired = expired
    )
}

fun ProductAgreementRegistration.toDTO(): ProductAgreementRegistrationDTO {
    return ProductAgreementRegistrationDTO(
        id = id,
        productId = productId,
        seriesId = seriesId,
        title = title,
        articleName = articleName,
        supplierId = supplierId,
        supplierRef = supplierRef,
        hmsArtNr = hmsArtNr,
        agreementId = agreementId,
        reference = reference,
        post = post,
        rank = rank,
        status = status,
        createdBy = createdBy,
        created = created,
        updated = updated,
        published = published,
        expired = expired
    )
}

fun ProductAgreementRegistration.toInfo() = AgreementInfo(
    id = agreementId, reference = reference, postNr = post, rank = rank, expired = expired
)

fun ProductAgreementRegistrationDTO.toInfo() = AgreementInfo(
    id = agreementId, reference = reference, postNr = post, rank = rank, expired = expired
)

