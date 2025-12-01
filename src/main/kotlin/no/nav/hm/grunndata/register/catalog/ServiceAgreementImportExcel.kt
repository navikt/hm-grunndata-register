package no.nav.hm.grunndata.register.catalog

import io.micronaut.security.authentication.Authentication
import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RapidDTO
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationDTO
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationService
import no.nav.hm.grunndata.register.event.EventPayload
import no.nav.hm.grunndata.register.servicejob.ServiceAgreement
import no.nav.hm.grunndata.register.servicejob.ServiceAgreementRepository
import no.nav.hm.grunndata.register.servicejob.ServiceJobRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Singleton
class ServiceAgreementImportExcel(private val serviceJobService: AgreementRegistrationService,
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

    suspend fun persistResult(serviceAgreementImportResul: ServiceAgreementMappedResultLists) {
        val updated = serviceAgreementImportResul.updateList.map {
            persistServiceAgreement(it)
        }
        val inserted = serviceAgreementImportResul.insertList.map {
            persistServiceAgreement(it)
        }
        val deactivated = serviceAgreementImportResul.deactivateList.map {
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
}

data class ServiceAgreementMappedResultLists(
    val updateList: List<ServiceAgreementDTO> = emptyList(),
    val insertList: List<ServiceAgreementDTO> = emptyList(),
    val deactivateList: List<ServiceAgreementDTO> = emptyList()
)




data class ServiceAgreementDTO (
    override val id: UUID = UUID.randomUUID(),
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
    override val updatedByUser: String = "system",
): EventPayload {
    override fun toRapidDTO(): RapidDTO {
        TODO("Not yet implemented")
    }
}

enum class ServiceAgreementStatus {
    ACTIVE, INACTIVE
}
