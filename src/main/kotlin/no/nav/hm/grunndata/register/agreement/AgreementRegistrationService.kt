package no.nav.hm.grunndata.register.agreement

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import jakarta.inject.Singleton
import java.util.UUID
import javax.transaction.Transactional

@Singleton
open class AgreementRegistrationService(private val agreementRegistrationRepository: AgreementRegistrationRepository,
                                        private val agreementRegistrationHandler: AgreementRegistrationHandler) {


    open suspend fun findById(id: UUID): AgreementRegistrationDTO? = agreementRegistrationRepository.findById(id)?.toDTO()

    open suspend fun save(dto: AgreementRegistrationDTO): AgreementRegistrationDTO =
        agreementRegistrationRepository.save(dto.toEntity()).toDTO()

    open suspend fun update(dto: AgreementRegistrationDTO): AgreementRegistrationDTO =
        agreementRegistrationRepository.update(dto.toEntity()).toDTO()

    @Transactional
    open suspend fun saveAndPushToRapid(dto: AgreementRegistrationDTO, isUpdate:Boolean): AgreementRegistrationDTO {
        val saved = if (isUpdate) update(dto) else save(dto)
        agreementRegistrationHandler.pushToRapidIfNotDraft(dto)
        return saved
    }

    open suspend fun findAll(spec: PredicateSpecification<AgreementRegistration>?, pageable: Pageable): Page<AgreementRegistrationDTO>  =
        agreementRegistrationRepository.findAll(spec, pageable).map { it.toDTO() }




}