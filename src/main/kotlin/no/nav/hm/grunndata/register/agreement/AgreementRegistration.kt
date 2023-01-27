package no.nav.hm.grunndata.register.agreement

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.annotation.Version
import io.micronaut.data.model.DataType
import no.nav.hm.grunndata.register.product.DraftStatus
import no.nav.hm.grunndata.register.product.Media
import no.nav.hm.grunndata.register.product.REGISTER
import java.time.LocalDateTime
import java.util.*

@MappedEntity("agreement_reg_v1")
data class AgreementRegistration(
    @field:Id
    val id: UUID,
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val title: String,
    val reference: String,
    val status : AgreementStatus = AgreementStatus.INACTIVE,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val published: LocalDateTime = LocalDateTime.now(),
    val expired: LocalDateTime = LocalDateTime.now(),
    val createdByUser: String,
    val updatedByUser: String,
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    @field:TypeDef(type = DataType.JSON)
    val agreementDTO: AgreementDTO,
    @field:Version
    val version: Long? = 0L
)

data class AgreementRegistrationDTO (
    val id: UUID,
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val title: String,
    val reference: String,
    val status : AgreementStatus = AgreementStatus.INACTIVE,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val published: LocalDateTime = LocalDateTime.now(),
    val expired: LocalDateTime = LocalDateTime.now(),
    val createdByUser: String,
    val updatedByUser: String,
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    val agreementDTO: AgreementDTO,
    val version: Long? = 0L
)

enum class AgreementStatus {
    INACTIVE, ACTIVE
}

data class AgreementPost (
    val identifier: String,
    val nr: Int,
    val title: String,
    val description: String,
    val created: LocalDateTime = LocalDateTime.now()
)

data class AgreementAttachment (
    val title: String?,
    val media: List<Media> = emptyList(),
    val description: String?,
)

data class AgreementDTO(
    val id: UUID,
    val identifier: String,
    val title: String,
    val resume: String?,
    val text: String?,
    val reference: String,
    val published: LocalDateTime,
    val expired: LocalDateTime,
    val attachments: List<AgreementAttachment> = emptyList(),
    val posts: List<AgreementPost>,
    val createdBy:String,
    val updatedBy: String,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
)

fun AgreementRegistration.toDTO(): AgreementRegistrationDTO = AgreementRegistrationDTO(
    id = id, draftStatus = draftStatus, title = title, reference = reference, status = status, created = created,
    updated = updated, published = published, expired = expired, createdByUser = createdByUser,
    updatedByUser = updatedByUser, createdBy= createdBy, updatedBy = updatedBy, agreementDTO = agreementDTO,
    version = version
)

fun AgreementRegistrationDTO.toEntity(): AgreementRegistration = AgreementRegistration(
    id = id, draftStatus = draftStatus, title = title, reference = reference, status = status, created = created,
    updated = updated, published = published, expired = expired, createdByUser = createdByUser,
    updatedByUser = updatedByUser, createdBy= createdBy, updatedBy = updatedBy, agreementDTO = agreementDTO,
    version = version
)