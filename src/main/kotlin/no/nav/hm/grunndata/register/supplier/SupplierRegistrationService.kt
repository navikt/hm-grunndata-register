package no.nav.hm.grunndata.register.supplier

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import java.util.UUID
import kotlinx.coroutines.flow.map
import no.nav.hm.grunndata.rapid.event.EventName

@Singleton
open class SupplierRegistrationService(
    private val supplierRepository: SupplierRepository,
    private val supplierRegistrationHandler: SupplierRegistrationHandler,
) {
    companion object {
        var CACHE: MutableMap<UUID, SupplierRegistrationDTO> = mutableMapOf()
    }

    open suspend fun findById(id: UUID): SupplierRegistrationDTO? = supplierRepository.findById(id)?.toDTO()

    open suspend fun findByName(name: String): SupplierRegistrationDTO? = supplierRepository.findByName(name)?.toDTO()

    open suspend fun findByIdentifier(identifier: String): SupplierRegistrationDTO? =
        supplierRepository.findByIdentifier(identifier)?.toDTO()

    open suspend fun update(
        dto: SupplierRegistrationDTO,
        id: UUID = dto.id,
    ) = supplierRepository.update(dto.toEntity()).toDTO()

    open suspend fun save(
        dto: SupplierRegistrationDTO,
        id: UUID = dto.id,
    ) = supplierRepository.save(dto.toEntity()).toDTO()

    @Transactional
    open suspend fun saveAndCreateEventIfNotDraft(
        supplier: SupplierRegistrationDTO,
        isUpdate: Boolean,
    ): SupplierRegistrationDTO {
        val saved = if (isUpdate) update(supplier) else save(supplier)
        supplierRegistrationHandler.queueDTORapidEvent(saved, eventName = EventName.registeredSupplierV1)
        return saved
    }

    open suspend fun findAll(
        spec: PredicateSpecification<SupplierRegistration>?,
        pageable: Pageable,
    ): Page<SupplierRegistrationDTO> = supplierRepository.findAll(spec, pageable).map { it.toDTO() }

}
