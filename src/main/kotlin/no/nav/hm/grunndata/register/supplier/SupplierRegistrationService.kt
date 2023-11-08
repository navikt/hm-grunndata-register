package no.nav.hm.grunndata.register.supplier

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
import java.util.*

@Singleton
open class SupplierRegistrationService(private val supplierRepository: SupplierRepository,
                                       private val supplierRegistrationHandler: SupplierRegistrationHandler,
                                       private val eventItemService: EventItemService) {

    open suspend fun findById(id: UUID): SupplierRegistrationDTO? = supplierRepository.findById(id)?.toDTO()

    open suspend fun findByName(name: String): SupplierRegistrationDTO?= supplierRepository.findByName(name)?.toDTO()

    open suspend fun update(dto: SupplierRegistrationDTO, id: UUID = dto.id) =
        supplierRepository.update(dto.toEntity()).toDTO()

    open suspend fun save(dto: SupplierRegistrationDTO, id: UUID = dto.id) =
        supplierRepository.save(dto.toEntity()).toDTO()

    @Transactional
    open suspend fun saveAndCreateEventIfNotDraft(supplier: SupplierRegistrationDTO, isUpdate: Boolean): SupplierRegistrationDTO {
        val saved = if (isUpdate) update(supplier) else save(supplier)
        if (saved.draftStatus == DraftStatus.DONE) {
            eventItemService.createNewEventItem(
                type = EventItemType.SUPPLIER,
                oid = saved.id,
                byUser = saved.updatedByUser,
                eventName = EventName.registeredSupplierV1,
                payload = saved)
        }
        return saved
    }

    open suspend fun findAll(spec: PredicateSpecification<SupplierRegistration>?, pageable: Pageable): Page<SupplierRegistrationDTO> =
    supplierRepository.findAll(spec, pageable).map { it.toDTO() }

    fun handleEventItem(eventItem: EventItem) {
        val dto = eventItem.payload as SupplierRegistrationDTO
        supplierRegistrationHandler.pushToRapid(dto, eventItem.extraKeyValues)
    }
}
