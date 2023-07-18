package no.nav.hm.grunndata.register.supplier

import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.Cacheable
import jakarta.inject.Singleton
import java.util.*

@Singleton
@CacheConfig("suppliers")
open class SupplierService(private val supplierRepository: SupplierRepository) {

    @Cacheable
    open suspend fun findById(id: UUID): SupplierRegistration? = supplierRepository.findById(id)

    @CacheInvalidate(parameters = ["id"])
    open suspend fun update(supplierRegistration: SupplierRegistration, id: UUID = supplierRegistration.id) = supplierRepository.update(supplierRegistration)

    @CacheInvalidate(parameters = ["id"])
    open suspend fun save(supplierRegistration: SupplierRegistration, id: UUID = supplierRegistration.id) = supplierRepository.save(supplierRegistration)

}
