package no.nav.hm.grunndata.register.agreement

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import java.time.LocalDateTime
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.AgreementStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.event.EventName

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

    open suspend fun findByReferenceLike(reference: String): AgreementRegistrationDTO? =
        agreementRegistrationRepository.findByReferenceLike(reference)?.toDTO()

    open suspend fun findAgreementsToBePublish(): List<AgreementRegistrationDTO> = agreementRegistrationRepository
        .findByDraftStatusAndAgreementStatusAndPublishedBeforeAndExpiredAfter(
            draftStatus = DraftStatus.DONE,
            status = AgreementStatus.INACTIVE,
            published = LocalDateTime.now(),
            expired = LocalDateTime.now())
        .map { it.toDTO() }

    open suspend fun findExpiringAgreements() = agreementRegistrationRepository.findByDraftStatusAndAgreementStatusAndExpiredBefore(
            draftStatus = DraftStatus.DONE,
            status = AgreementStatus.ACTIVE,
            expired = LocalDateTime.now()
        ).map { it.toDTO() }


    suspend fun AgreementRegistration.toDTO(): AgreementRegistrationDTO = AgreementRegistrationDTO(
        id = id, draftStatus = draftStatus, agreementStatus = agreementStatus,
        title = title, reference = reference, created = created,
        updated = updated, published = published, expired = expired, createdByUser = createdByUser,
        updatedByUser = updatedByUser, createdBy = createdBy, updatedBy = updatedBy,
        agreementData = agreementData, version = version, pastAgreement = pastAgreement,
        delkontraktList = delkontraktRegistrationRepository.findByAgreementId(id).map { it.toDTO() }
    )
}