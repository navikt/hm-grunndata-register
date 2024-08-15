package no.nav.hm.grunndata.register.agreement

import jakarta.inject.Singleton
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.DelkontraktType

@Singleton
class DelkontraktRegistrationService(private val delkontraktRegistrationRepository: DelkontraktRegistrationRepository) {

    suspend fun findByAgreementId(agreementId: UUID): List<DelkontraktRegistrationDTO> =
        delkontraktRegistrationRepository.findByAgreementId(agreementId).map { it.toDTO() }

    suspend fun findByAgreementIdAndType(agreementId: UUID, type: DelkontraktType): DelkontraktRegistrationDTO? =
        delkontraktRegistrationRepository.findByAgreementIdAndType(agreementId, type)?.toDTO()

    suspend fun findById(id: UUID): DelkontraktRegistrationDTO? =
        delkontraktRegistrationRepository.findById(id)?.toDTO()

    suspend fun save(dto: DelkontraktRegistrationDTO): DelkontraktRegistrationDTO =
        delkontraktRegistrationRepository.save(dto.toEntity()).toDTO()

    suspend fun update(dto: DelkontraktRegistrationDTO): DelkontraktRegistrationDTO =
        delkontraktRegistrationRepository.update(dto.toEntity()).toDTO()

    suspend fun deleteById(id: UUID)  = delkontraktRegistrationRepository.deleteById(id)



}