package no.nav.hm.grunndata.register.agreement

import jakarta.inject.Singleton
import javax.transaction.Transactional

@Singleton
open class AgreementRegistrationService(private val agreementRegistrationRepository: AgreementRegistrationRepository,
                                        private val agreementRegstrationHandler: AgreementRegistrationHandler) {


    open suspend fun save(dto: AgreementRegistrationDTO): AgreementRegistrationDTO =
        agreementRegistrationRepository.save(dto.toEntity()).toDTO()

    open suspend fun update(dto: AgreementRegistrationDTO): AgreementRegistrationDTO =
        agreementRegistrationRepository.update(dto.toEntity()).toDTO()

    @Transactional
    open suspend fun saveAndPushToRapid(dto: AgreementRegistrationDTO, isUpdate:Boolean): AgreementRegistrationDTO {
        val saved = if (isUpdate) update(dto) else save(dto)
        agreementRegstrationHandler.pushToRapidIfNotDraft(dto)
        return saved
    }



}