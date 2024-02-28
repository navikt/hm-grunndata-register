package no.nav.hm.grunndata.register.agreement

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.rapid.dto.AgreementStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.event.EventName
import java.time.LocalDateTime
import java.util.*

@Singleton
open class AgreementRegistrationService(
    private val agreementRegistrationRepository: AgreementRegistrationRepository,
    private val agreementRegistrationEventHandler: AgreementRegistrationEventHandler,
    private val delkontraktRegistrationRepository: DelkontraktRegistrationRepository
) {


    open suspend fun findById(id: UUID): AgreementRegistrationDTO? =
        agreementRegistrationRepository.findById(id)?.toDTO()

    open suspend fun save(dto: AgreementRegistrationDTO): AgreementRegistrationDTO =
        agreementRegistrationRepository.save(dto.toEntity()).toDTO()

    open suspend fun update(dto: AgreementRegistrationDTO): AgreementRegistrationDTO =
        agreementRegistrationRepository.update(dto.toEntity()).toDTO()

    @Transactional
    open suspend fun saveAndCreateEventIfNotDraft(
        dto: AgreementRegistrationDTO,
        isUpdate: Boolean
    ): AgreementRegistrationDTO {
        val saved = if (isUpdate) update(dto) else save(dto)
        if (saved.draftStatus == DraftStatus.DONE) {
            agreementRegistrationEventHandler.queueDTORapidEvent(saved, eventName = EventName.registeredAgreementV1)
        }
        return saved
    }

    open suspend fun findAll(
        spec: PredicateSpecification<AgreementRegistration>?,
        pageable: Pageable
    ): Page<AgreementBasicInformationDto> =
        agreementRegistrationRepository.findAll(spec, pageable).map { it.toBasicInformationDto() }

    open suspend fun findByReference(reference: String): AgreementRegistrationDTO? =
        agreementRegistrationRepository.findByReference(reference)?.toDTO()

    open suspend fun findReference(reference: String): AgreementRegistrationDTO? =
        agreementRegistrationRepository.findByReference(reference)?.toDTO()

    open suspend fun findByAgreementStatusAndExpiredBefore(
        status: AgreementStatus,
        expired: LocalDateTime? = LocalDateTime.now()
    ) = agreementRegistrationRepository.findByDraftStatusAndAgreementStatusAndExpiredBefore(
        status = status,
        expired = expired
    ).map { it.toDTO() }

    open suspend fun findByAgreementStatusAndPublishedBeforeAndExpiredAfter(
        status: AgreementStatus,
        published: LocalDateTime? = LocalDateTime.now(),
        expired: LocalDateTime? = LocalDateTime.now()
    ) = agreementRegistrationRepository.findByDraftStatusAndAgreementStatusAndPublishedBeforeAndExpiredAfter(
        status = status,
        published = published,
        expired = expired
    ).map { it.toDTO() }

    open suspend fun deleteById(id: UUID) = agreementRegistrationRepository.deleteById(id)

    suspend fun AgreementRegistration.toDTO(): AgreementRegistrationDTO = AgreementRegistrationDTO(
        id = id, draftStatus = draftStatus, agreementStatus = agreementStatus,
        title = title, reference = reference, created = created,
        updated = updated, published = published, expired = expired, createdByUser = createdByUser,
        updatedByUser = updatedByUser, createdBy = createdBy, updatedBy = updatedBy,
        agreementData = agreementData, version = version, delkontraktList = delkontraktRegistrationRepository.findByAgreementId(id).map { it.toDTO() }
    )
}