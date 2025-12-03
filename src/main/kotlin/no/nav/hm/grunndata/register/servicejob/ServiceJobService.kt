package no.nav.hm.grunndata.register.servicejob

import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.ServiceAgreementInfo
import no.nav.hm.grunndata.rapid.event.EventName

@Singleton
open class ServiceJobService(
    private val serviceJobRepository: ServiceJobRepository,
    private val serviceJobEventHandler: ServiceJobEventHandler,
    private val serviceAgreementRepository: ServiceAgreementRepository
) {

    open suspend fun saveAndCreateEventIfNotDraft(
        serviceJob: ServiceJob,
        isUpdate: Boolean = false
    ): ServiceJob {
        val saved = if (isUpdate) update(serviceJob) else save(serviceJob)
        if (saved.draftStatus == DraftStatus.DONE) {
            serviceJobEventHandler.queueDTORapidEvent(saved.toDTO(), eventName = EventName.registeredServiceJobV1)
        }
        return saved
    }

    open suspend fun queueServiceJobEventIfNotDraft(
        serviceJob: ServiceJob
    ) {
        if (serviceJob.draftStatus == DraftStatus.DONE) {
            serviceJobEventHandler.queueDTORapidEvent(serviceJob.toDTO(), eventName = EventName.registeredServiceJobV1)
        }
    }
    private suspend fun ServiceJob.toDTO(): ServiceJobDTO {
        val agreements = serviceAgreementRepository.findByServiceId(id).map { agree ->
            ServiceAgreementInfo(
                id = agree.id,
                supplierId = supplierId,
                supplierRef = supplierRef,
                agreementId = agree.agreementId,
                status = agree.status,
                published = agree.published,
                expired = agree.expired,
                serviceId = agree.serviceId
            )
        }
        return ServiceJobDTO(
            id = id,
            title = title,
            supplierId = supplierId,
            supplierRef = supplierRef,
            hmsNr = hmsArtNr,
            isoCategory = isoCategory,
            published = published,
            expired = expired,
            updated = updated,
            draftStatus = draftStatus,
            status = status,
            created = created,
            updatedBy = updatedBy,
            createdBy = createdBy,
            createdByUser = createdByUser,
            updatedByUser = updatedByUser,
            attributes = attributes,
            agreements = agreements,
            version = version
        )
    }

    private suspend fun save(serviceJob: ServiceJob): ServiceJob {
        return serviceJobRepository.save(serviceJob)
    }

    private suspend fun update(serviceJob: ServiceJob): ServiceJob {
        return serviceJobRepository.update(serviceJob)
    }
}