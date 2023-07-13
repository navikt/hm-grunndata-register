package no.nav.hm.grunndata.register.agreement

import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AgreementDTO
import no.nav.hm.grunndata.rapid.dto.AgreementRegistrationRapidDTO
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.RegisterRapidPushService

@Singleton
class AgreementRegistrationHandler(private val registerRapidPushService: RegisterRapidPushService) {

    fun pushToRapidIfNotDraft(dto: AgreementRegistrationDTO) {
        runBlocking {
            if (dto.draftStatus == DraftStatus.DONE) {
                registerRapidPushService.pushDTOToKafka(dto.toRapidDTO(), EventName.registerAgreementV1)
            }
        }
    }

    private fun AgreementRegistrationDTO.toRapidDTO(): AgreementRegistrationRapidDTO = AgreementRegistrationRapidDTO(
        id = id, draftStatus = draftStatus, created = created, updated = updated, published = published, expired = expired, createdByUser = createdByUser,
        updatedByUser = updatedByUser, createdBy = createdBy, updatedBy = updatedBy, version = version, agreementDTO = agreementData.toDTO(this)
    )

    private fun AgreementData.toDTO(registration: AgreementRegistrationDTO): AgreementDTO = AgreementDTO(
        id = registration.id, identifier = identifier, title = registration.title, resume = resume, text = text,
        status = registration.agreementStatus, reference = registration.reference, published = registration.published,
        expired = registration.expired, attachments = attachments, posts = posts, createdBy = registration.createdBy,
        updatedBy = registration.updatedBy, created = registration.created, updated = registration.updated
    )

}