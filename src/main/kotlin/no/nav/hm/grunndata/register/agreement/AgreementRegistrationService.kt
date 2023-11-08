package no.nav.hm.grunndata.register.agreement

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.event.EventItem
import no.nav.hm.grunndata.register.event.EventItemService
import no.nav.hm.grunndata.register.event.EventItemType
import java.util.UUID

@Singleton
open class AgreementRegistrationService(private val agreementRegistrationRepository: AgreementRegistrationRepository,
                                        private val agreementRegistrationHandler: AgreementRegistrationHandler,
                                        private val eventItemService: EventItemService) {


    open suspend fun findById(id: UUID): AgreementRegistrationDTO? = agreementRegistrationRepository.findById(id)?.toDTO()

    open suspend fun save(dto: AgreementRegistrationDTO): AgreementRegistrationDTO =
        agreementRegistrationRepository.save(dto.toEntity()).toDTO()

    open suspend fun update(dto: AgreementRegistrationDTO): AgreementRegistrationDTO =
        agreementRegistrationRepository.update(dto.toEntity()).toDTO()

    @Transactional
    open suspend fun saveAndCreateEventIfNotDraft(dto: AgreementRegistrationDTO, isUpdate:Boolean): AgreementRegistrationDTO {
        val saved = if (isUpdate) update(dto) else save(dto)
        if (saved.draftStatus == DraftStatus.DONE) {
            eventItemService.createNewEventItem(
                type = EventItemType.AGREEMENT,
                oid = saved.id,
                byUser = saved.updatedByUser,
                eventName = EventName.registeredAgreementV1,
                payload = saved
            )
        }
        return saved
    }

    open suspend fun findAll(spec: PredicateSpecification<AgreementRegistration>?, pageable: Pageable): Page<AgreementRegistrationDTO>  =
        agreementRegistrationRepository.findAll(spec, pageable).map { it.toDTO() }


    open suspend fun findByReference(reference: String): AgreementRegistrationDTO? =
        agreementRegistrationRepository.findByReference(reference)?.toDTO()

    fun handleEventItem(eventItem: EventItem) {
        val dto = eventItem.payload as AgreementRegistrationDTO
        agreementRegistrationHandler.pushToRapid(dto)
    }

}