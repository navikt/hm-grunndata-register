package no.nav.hm.grunndata.register.agreement

import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.DelkontraktType
import java.util.UUID

@Singleton
class DelkontraktRegistrationService(
    private val delkontraktRegistrationRepository: DelkontraktRegistrationRepository,
    private val agreementRegistrationService: AgreementRegistrationService,
) {
    suspend fun findByAgreementId(agreementId: UUID): List<DelkontraktRegistrationDTO> =
        delkontraktRegistrationRepository.findByAgreementId(agreementId).map { it.toDTO() }

    suspend fun findById(id: UUID): DelkontraktRegistrationDTO? = delkontraktRegistrationRepository.findById(id)?.toDTO()

    suspend fun save(dto: DelkontraktRegistrationDTO): DelkontraktRegistrationDTO {
        val created = delkontraktRegistrationRepository.save(dto.toEntity()).toDTO()
        agreementRegistrationService.findById(created.agreementId)?.let {
            agreementRegistrationService.saveAndCreateEventIfNotDraft(it, true)
        }
        return created
    }

    suspend fun update(dto: DelkontraktRegistrationDTO): DelkontraktRegistrationDTO {
        val updated = delkontraktRegistrationRepository.update(dto.toEntity()).toDTO()
        agreementRegistrationService.findById(updated.agreementId)?.let {
            agreementRegistrationService.saveAndCreateEventIfNotDraft(it, true)
        }
        return updated
    }

    suspend fun deleteById(
        id: UUID,
        agreementId: UUID,
    ) {
        delkontraktRegistrationRepository.deleteById(id)
        agreementRegistrationService.findById(agreementId)?.let {
            agreementRegistrationService.saveAndCreateEventIfNotDraft(it, true)
        }
    }
}
