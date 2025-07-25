package no.nav.hm.grunndata.register.agreement

import io.micronaut.core.annotation.Generated
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.annotation.Version
import io.micronaut.data.model.DataType
import no.nav.hm.grunndata.rapid.dto.AgreementAttachment
import no.nav.hm.grunndata.rapid.dto.AgreementDTO
import no.nav.hm.grunndata.rapid.dto.AgreementPost
import no.nav.hm.grunndata.rapid.dto.AgreementRegistrationRapidDTO
import no.nav.hm.grunndata.rapid.dto.AgreementStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RapidDTO
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.event.EventPayload
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

@MappedEntity("agreement_reg_v1")
data class AgreementRegistration(
    @field:Id
    val id: UUID,
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val agreementStatus: AgreementStatus = AgreementStatus.INACTIVE,
    val title: String,
    val reference: String,
    @field:Generated
    @field:AutoPopulated(updatable = false)
    val agreementKey: String? = generateKey(reference),
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
    val previousAgreement: UUID? = null,
    @field:Version
    val version: Long? = 0L,
)

data class AgreementData(
    val resume: String?,
    val text: String?,
    val identifier: String,
    val attachments: List<AgreementAttachment> = emptyList(),
    @Deprecated("Use delkontraktList instead")
    val posts: List<AgreementPost> = emptyList(),
    val isoCategory: List<String> = emptyList(),
)

data class AgreementRegistrationDTO(
    override val id: UUID,
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val agreementStatus: AgreementStatus = AgreementStatus.INACTIVE,
    val title: String,
    val reference: String,
    val agreementKey: String = generateKey(reference),
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val published: LocalDateTime = LocalDateTime.now(),
    val expired: LocalDateTime = LocalDateTime.now(),
    val createdByUser: String,
    override val updatedByUser: String,
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    val agreementData: AgreementData,
    val delkontraktList: List<DelkontraktRegistrationDTO> = emptyList(),
    val previousAgreement: UUID? = null,
    val version: Long? = 0L,
) : EventPayload {
    override fun toRapidDTO(): RapidDTO =
        AgreementRegistrationRapidDTO(
            id = id,
            draftStatus = draftStatus,
            created = created,
            updated = updated,
            published = published,
            expired = expired,
            createdByUser = createdByUser,
            updatedByUser = updatedByUser,
            createdBy = createdBy,
            updatedBy = updatedBy,
            version = version,
            agreementDTO = agreementData.toDTO(this),
        )

    private fun AgreementData.toDTO(registration: AgreementRegistrationDTO): AgreementDTO =
        AgreementDTO(
            id = registration.id,
            identifier = identifier,
            title = registration.title,
            agreementKey = registration.agreementKey,
            resume = resume,
            text = text,
            status = registration.agreementStatus,
            reference = registration.reference,
            published = registration.published,
            expired = registration.expired,
            attachments = attachments,
            posts = registration.delkontraktList.map { it.toAgreementPost(registration) },
            createdBy = registration.createdBy,
            updatedBy = registration.updatedBy,
            created = registration.created,
            updated = registration.updated,
            isoCategory = isoCategory,
            previousAgreement = registration.previousAgreement,
        )
}

fun generateKey(seed: String): String = "${Random(seed.hashCode() + 42L).nextInt(Int.MAX_VALUE)}"

fun DelkontraktRegistrationDTO.toAgreementPost(agreement: AgreementRegistrationDTO): AgreementPost =
    AgreementPost(
        id = id,
        identifier = identifier,
        type = type,
        title = delkontraktData.title!!,
        description = delkontraktData.description!!,
        nr = delkontraktData.sortNr,
        refNr = delkontraktData.refNr,
        created = agreement.created,
    )

data class AgreementBasicInformationDto(
    val id: UUID,
    val title: String,
    val reference: String,
    val agreementStatus: AgreementStatus,
)

fun AgreementDTO.toData(): AgreementData =
    AgreementData(
        resume = resume,
        text = text,
        identifier = identifier,
        attachments = attachments,
        posts = posts,
        isoCategory = isoCategory,
    )

fun AgreementRegistration.toBasicInformationDto(): AgreementBasicInformationDto =
    AgreementBasicInformationDto(
        id = id,
        title = title,
        reference = reference,
        agreementStatus = agreementStatus,
    )

fun AgreementRegistrationDTO.toEntity(): AgreementRegistration =
    AgreementRegistration(
        id = id,
        draftStatus = draftStatus,
        agreementStatus = agreementStatus,
        title = title,
        reference = reference,
        agreementKey = agreementKey,
        created = created,
        updated = updated,
        published = published,
        expired = expired,
        createdByUser = createdByUser,
        updatedByUser = updatedByUser,
        createdBy = createdBy,
        updatedBy = updatedBy,
        agreementData = agreementData,
        version = version,
        previousAgreement = previousAgreement,
    )
