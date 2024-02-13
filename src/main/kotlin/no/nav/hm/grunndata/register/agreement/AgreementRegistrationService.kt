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
import java.util.UUID

@Singleton
open class AgreementRegistrationService(
    private val agreementRegistrationRepository: AgreementRegistrationRepository,
    private val agreementRegistrationEventHandler: AgreementRegistrationEventHandler
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

    open suspend fun findReferenceAndId(): List<AgreementPDTO> = agreementRegistrationRepository.find()

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

}