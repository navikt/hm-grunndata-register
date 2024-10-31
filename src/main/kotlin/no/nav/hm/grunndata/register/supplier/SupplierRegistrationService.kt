package no.nav.hm.grunndata.register.supplier

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.jpa.criteria.impl.expression.LiteralExpression
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.data.runtime.criteria.where
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import kotlinx.coroutines.flow.map
import no.nav.hm.grunndata.rapid.event.EventName
import java.util.UUID

@Singleton
open class SupplierRegistrationService(
    private val supplierRepository: SupplierRepository,
    private val supplierRegistrationHandler: SupplierRegistrationHandler,
) {
    companion object {
        var CACHE: MutableMap<UUID, SupplierRegistrationDTO> = mutableMapOf()
    }

    init {
        supplierRepository.findAll().map { CACHE.put(it.id, it.toDTO()) }
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
        params: Map<String, String>?,
        pageable: Pageable,
    ): Page<SupplierRegistrationDTO> = findAll(buildCriteriaSpec(params), pageable)

    open suspend fun findAll(
        spec: PredicateSpecification<SupplierRegistration>?,
        pageable: Pageable,
    ): Page<SupplierRegistrationDTO> = supplierRepository.findAll(spec, pageable).map { it.toDTO() }

    private fun buildCriteriaSpec(params: Map<String, String>?): PredicateSpecification<SupplierRegistration>? =
        params?.let {
            where {
                if (params.contains("status")) root[SupplierRegistration::status] eq params["status"]
            }.and { root, criteriaBuilder ->
                if (params.contains("name")) {
                    criteriaBuilder.like(root[SupplierRegistration::name], LiteralExpression("%${params["name"]}%"))
                } else {
                    null
                }
            }
        }

    open suspend fun findNameAndId(): List<SupplierNameAndId> = supplierRepository.findNameAndId()
}
