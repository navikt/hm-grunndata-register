package no.nav.hm.grunndata.register.product

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.*

@JdbcRepository(dialect = Dialect.POSTGRES)
interface ProductRegistrationRepository : CoroutineCrudRepository<ProductRegistration, UUID>,
    CoroutineJpaSpecificationExecutor<ProductRegistration> {

    suspend fun findByIdAndSupplierId(id:UUID, supplierId: UUID): ProductRegistration?

    suspend fun findBySupplierId(supplierId: UUID, pageable: Pageable): Page<ProductRegistration>

    suspend fun findByHmsArtNrAndSupplierId(hmsArtNr: String, supplierId: UUID): ProductRegistration?

    suspend fun findBySupplierIdAndSupplierRef(supplierId: UUID, supplierRef: String): ProductRegistration?

}
