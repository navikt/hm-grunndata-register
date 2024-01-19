package no.nav.hm.grunndata.register.product

import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.*

@JdbcRepository(dialect = Dialect.POSTGRES)
@Join(value = "agreements", type = Join.Type.LEFT_FETCH)
interface ProductRegistrationRepository : CoroutineCrudRepository<ProductRegistration, UUID>,
    CoroutineJpaSpecificationExecutor<ProductRegistration> {
    suspend fun findByIdAndSupplierId(id:UUID, supplierId: UUID): ProductRegistration?


    suspend fun findByHmsArtNrAndSupplierId(hmsArtNr: String, supplierId: UUID): ProductRegistration?

    suspend fun findBySupplierRefAndSupplierId(supplierRef: String, supplierId: UUID): ProductRegistration?

    suspend fun findBySeriesIdAndSupplierId(seriesId: String, supplierId: UUID): List<ProductRegistration>


    suspend fun findBySeriesId(seriesId: String): List<ProductRegistration>



}
