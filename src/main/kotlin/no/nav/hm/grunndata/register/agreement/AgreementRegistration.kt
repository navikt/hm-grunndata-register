package no.nav.hm.grunndata.register.agreement

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.annotation.Version
import io.micronaut.data.model.DataType
import no.nav.hm.grunndata.rapid.dto.*
import no.nav.hm.grunndata.register.REGISTER

import java.time.LocalDateTime
import java.util.*

@MappedEntity("agreement_reg_v1")
data class AgreementRegistration(
    @field:Id
    val id: UUID,
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val agreementStatus: AgreementStatus = AgreementStatus.INACTIVE,
    val title: String,
    val reference: String,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val published: LocalDateTime = LocalDateTime.now(),
    val expired: LocalDateTime = LocalDateTime.now(),
    val createdByUser: String,
    val updatedByUser: String,
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    @field:TypeDef(type = DataType.JSON)
    val agreementData: AgreementData,
    @field:Version
    val version: Long? = 0L
)

data class AgreementData (
    val resume: String?,
    val text: String?,
    val identifier: String,
    val attachments: List<AgreementAttachment> = emptyList(),
    val posts: List<AgreementPost> = emptyList(),
)

data class AgreementRegistrationDTO (
    val id: UUID,
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val agreementStatus: AgreementStatus = AgreementStatus.INACTIVE,
    val title: String,
    val reference: String,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val published: LocalDateTime = LocalDateTime.now(),
    val expired: LocalDateTime = LocalDateTime.now(),
    val createdByUser: String,
    val updatedByUser: String,
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    val agreementData: AgreementData,
    val version: Long? = 0L
)

fun AgreementDTO.toData(): AgreementData = AgreementData(
    resume = resume, text = text, identifier = identifier, attachments = attachments, posts = posts
)

fun AgreementRegistration.toDTO(): AgreementRegistrationDTO = AgreementRegistrationDTO(
    id = id, draftStatus = draftStatus, title = title, reference = reference, created = created,
    updated = updated, published = published, expired = expired, createdByUser = createdByUser,
    updatedByUser = updatedByUser, createdBy= createdBy, updatedBy = updatedBy,
    agreementData = agreementData, version = version
)

fun AgreementRegistrationDTO.toEntity(): AgreementRegistration = AgreementRegistration(
    id = id, draftStatus = draftStatus, title = title, reference = reference, created = created,
    updated = updated, published = published, expired = expired, createdByUser = createdByUser,
    updatedByUser = updatedByUser, createdBy= createdBy, updatedBy = updatedBy,
    agreementData = agreementData, version = version
)
