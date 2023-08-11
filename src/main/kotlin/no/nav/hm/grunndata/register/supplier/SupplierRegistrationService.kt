package no.nav.hm.grunndata.register.supplier

import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import kotlinx.coroutines.flow.map
import no.nav.hm.grunndata.register.product.toDTO
import java.util.*

@Singleton
@CacheConfig("suppliers")
open class SupplierRegistrationService(private val supplierRepository: SupplierRepository,
                                       private val supplierRegistrationHandler: SupplierRegistrationHandler) {

    @Cacheable
    open suspend fun findById(id: UUID): SupplierRegistrationDTO? = supplierRepository.findById(id)?.toDTO()

    open suspend fun findByName(name: String): SupplierRegistrationDTO?= supplierRepository.findByName(name)?.toDTO()

    @CacheInvalidate(parameters = ["id"])
    open suspend fun update(dto: SupplierRegistrationDTO, id: UUID = dto.id) =
        supplierRepository.update(dto.toEntity()).toDTO()

    @CacheInvalidate(parameters = ["id"])
    open suspend fun save(dto: SupplierRegistrationDTO, id: UUID = dto.id) =
        supplierRepository.save(dto.toEntity()).toDTO()

    @Transactional
    open suspend fun saveAndPushToRapidIfNotDraft(supplier: SupplierRegistrationDTO, isUpdate: Boolean): SupplierRegistrationDTO {
        val saved = if (isUpdate) update(supplier) else save(supplier)
        supplierRegistrationHandler.pushToRapidIfNotDraft(saved)
        return saved
    }

    open suspend fun findAll(spec: PredicateSpecification<SupplierRegistration>?, pageable: Pageable): Page<SupplierRegistrationDTO> =
    supplierRepository.findAll(spec, pageable).map { it.toDTO() }

}
