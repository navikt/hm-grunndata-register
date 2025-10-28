package no.nav.hm.grunndata.register.supplier

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.UUID

object SupplierRegistrationCache {

    @Volatile
    private var cache: Map<UUID, SupplierRegistrationDTO> = emptyMap()
    private var supplierRepository: SupplierRepository? = null
    private val LOG = LoggerFactory.getLogger(SupplierRegistrationCache::class.java)

    operator fun get(supplierId: UUID): SupplierRegistrationDTO? {
        if (cache[supplierId] == null && supplierRepository != null) {
            synchronized(this) {
                if (cache[supplierId] == null) {
                    runBlocking {
                        refresh(supplierRepository!!)
                    }
                }
            }
        }
        return cache[supplierId]
    }

    suspend fun refresh(repository: SupplierRepository) {
        LOG.info("Refreshing SupplierRegistrationCache")
        supplierRepository = repository
        cache = repository.findAll()
            .map { it.id to it.toDTO() }
            .toList()
            .toMap()
    }

}