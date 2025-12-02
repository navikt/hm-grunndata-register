package no.nav.hm.grunndata.register.catalog

import io.micronaut.security.authentication.Authentication
import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.ServiceAgreementInfo
import no.nav.hm.grunndata.rapid.dto.ServiceAgreementStatus
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationDTO
import no.nav.hm.grunndata.register.servicejob.ServiceAgreement
import no.nav.hm.grunndata.register.servicejob.ServiceAgreementRepository
import no.nav.hm.grunndata.register.servicejob.ServiceJob
import no.nav.hm.grunndata.register.servicejob.ServiceJobDTO
import no.nav.hm.grunndata.register.servicejob.ServiceJobEventHandler
import no.nav.hm.grunndata.register.servicejob.ServiceJobRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Singleton
class ServiceAgreementImportExcel(private val serviceJobEventHandler: ServiceJobEventHandler,
                                  private val serviceAgreementRepository: ServiceAgreementRepository,
                                  private val serviceJobRepository: ServiceJobRepository)  {

    suspend fun mapToServiceAgreementImportResult(
        serviceImportResult: CatalogImportResult,
        agreement: AgreementRegistrationDTO,
        authentication: Authentication,
        supplierId: UUID
    ): ServiceAgreementMappedResultLists {
        val updatedList =
            serviceImportResult.updatedList.map { it.toServiceAgreementDTO(agreement, authentication, supplierId) }
        val insertedList =
            serviceImportResult.insertedList.map { it.toServiceAgreementDTO(agreement, authentication, supplierId) }
        val deactivatedList =
            serviceImportResult.deactivatedList.map { it.toServiceAgreementDTO(agreement, authentication, supplierId) }
        return ServiceAgreementMappedResultLists(updatedList, insertedList, deactivatedList)
    }

    suspend fun persistResult(serviceAgreementImportResult: ServiceAgreementMappedResultLists) {
        val updated = serviceAgreementImportResult.updateList.map {
            persistServiceAgreement(it)
        }
        val inserted = serviceAgreementImportResult.insertList.map {
            persistServiceAgreement(it)
        }
        val deactivated = serviceAgreementImportResult.deactivateList.map {
            serviceAgreementRepository.findByServiceIdAndAgreementId(it.serviceId, it.agreementId)?.let { existing ->
                serviceAgreementRepository.update(
                    existing.copy(
                        status = ServiceAgreementStatus.INACTIVE,
                        expired = LocalDateTime.now(),
                        updated = LocalDateTime.now()
                    )
                )
            } ?: serviceAgreementRepository.save(
            ServiceAgreement(
                serviceId = it.serviceId,
                supplierId = it.supplierId,
                supplierRef = it.supplierRef,
                agreementId = it.agreementId,
                status = ServiceAgreementStatus.INACTIVE,
                published = it.published,
                expired = LocalDateTime.now()
            ))
        }
        val distinct = (updated + inserted + deactivated).distinctBy { it.serviceId }
        distinct.forEach {
            val service = serviceJobRepository.findById(it.serviceId)
            serviceJobEventHandler.queueDTORapidEvent(service!!.toDTO(), eventName = EventName.registeredServiceJobV1)
        }
    }

    private suspend fun persistServiceAgreement(serviceAg: ServiceAgreementDTO) =
        serviceAgreementRepository.findByServiceIdAndAgreementId(serviceAg.serviceId, serviceAg.agreementId)
            ?.let { existing ->
                serviceAgreementRepository.update(
                    existing.copy(
                        supplierRef = serviceAg.supplierRef,
                        status = serviceAg.status,
                        updated = LocalDateTime.now(),
                        published = serviceAg.published,
                        expired = serviceAg.expired
                    )
                )
            } ?: serviceAgreementRepository.save(
            ServiceAgreement(
                serviceId = serviceAg.serviceId,
                supplierId = serviceAg.supplierId,
                supplierRef = serviceAg.supplierRef,
                agreementId = serviceAg.agreementId,
                status = serviceAg.status,
                published = serviceAg.published,
                expired = serviceAg.expired
            )
        )


    private suspend fun CatalogImport.toServiceAgreementDTO(
        agreement: AgreementRegistrationDTO,
        authentication: Authentication,
        supplierId: UUID
    ): ServiceAgreementDTO {
        val service = serviceJobRepository.findBySupplierIdAndHmsArtNr(supplierId, hmsArtNr)
            ?: throw IllegalStateException("Service with hmsArtNr $hmsArtNr for supplier $supplierId not found when importing service agreement from catalog")
        return ServiceAgreementDTO(
            serviceId = service.id,
            title = title,
            isoCategory = iso,
            supplierId = supplierId,
            supplierRef = supplierRef,
            agreementId = agreement.id,
            createdBy = authentication.name,
            updatedBy = authentication.name,
            published = dateFrom.atStartOfDay(),
            expired = dateTo.atStartOfDay(),
            status = mapServiceAgreementStatus(agreement, dateFrom, dateTo),
        )
    }
    private fun mapServiceAgreementStatus(
        agreement: AgreementRegistrationDTO,
        dateFrom: LocalDate,
        dateTo: LocalDate
    ): ServiceAgreementStatus {
        val nowDate = LocalDate.now()
        return if (agreement.draftStatus == DraftStatus.DONE
            && dateFrom <= nowDate
            && dateTo > nowDate
        ) ServiceAgreementStatus.ACTIVE
        else ServiceAgreementStatus.INACTIVE
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
}


data class ServiceAgreementMappedResultLists(
    val updateList: List<ServiceAgreementDTO> = emptyList(),
    val insertList: List<ServiceAgreementDTO> = emptyList(),
    val deactivateList: List<ServiceAgreementDTO> = emptyList()
)




data class ServiceAgreementDTO (
    val id: UUID = UUID.randomUUID(),
    val serviceId: UUID,
    val title: String,
    val isoCategory: String? = null,
    val supplierId: UUID,
    val supplierRef: String? = null,
    val agreementId: UUID,
    val status: ServiceAgreementStatus = ServiceAgreementStatus.INACTIVE,
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val published: LocalDateTime,
    val expired: LocalDateTime,
    val updatedByUser: String = "system",
)