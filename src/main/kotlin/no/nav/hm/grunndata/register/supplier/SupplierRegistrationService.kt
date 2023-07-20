package no.nav.hm.grunndata.register.supplier

import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.Cacheable
import jakarta.inject.Singleton
import java.util.*
import javax.transaction.Transactional

@Singleton
@CacheConfig("suppliers")
open class SupplierRegistrationService(private val supplierRepository: SupplierRepository,
                                       private val supplierRegistrationHandler: SupplierRegistrationHandler) {

    @Cacheable
    open suspend fun findById(id: UUID): SupplierRegistrationDTO? = supplierRepository.findById(id)?.toDTO()

    @CacheInvalidate(parameters = ["id"])
    open suspend fun update(dto: SupplierRegistrationDTO, id: UUID = dto.id) =
        supplierRepository.update(dto.toEntity()).toDTO()

    @CacheInvalidate(parameters = ["id"])
    open suspend fun save(dto: SupplierRegistrationDTO, id: UUID = dto.id) =
        supplierRepository.save(dto.toEntity()).toDTO()

    @Transactional
    open suspend fun saveAndPushToRapid(supplier: SupplierRegistrationDTO, isUpdate: Boolean): SupplierRegistrationDTO {
        val saved = if (isUpdate) update(supplier) else save(supplier)
        supplierRegistrationHandler.pushToRapidIfNotDraft(saved)
        return saved
    }

}