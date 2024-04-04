package no.nav.hm.grunndata.register.product

import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.*

@JdbcRepository(dialect = Dialect.POSTGRES)
interface ProductRegistrationRepository : CoroutineCrudRepository<ProductRegistration, UUID>,
    CoroutineJpaSpecificationExecutor<ProductRegistration> {
    suspend fun findByIdAndSupplierId(id:UUID, supplierId: UUID): ProductRegistration?


    suspend fun findByHmsArtNrAndSupplierId(hmsArtNr: String, supplierId: UUID): ProductRegistration?

    suspend fun findByHmsArtNr(hmsArtNr: String): ProductRegistration?

    suspend fun findBySupplierRefAndSupplierId(supplierRef: String, supplierId: UUID): ProductRegistration?

    suspend fun findBySupplierId(supplierId: UUID): List<ProductRegistration>

    suspend fun findBySeriesIdAndSupplierId(seriesId: String, supplierId: UUID): List<ProductRegistration>

    suspend fun findBySeriesId(seriesId: String): List<ProductRegistration>


    @Query("SELECT * FROM product_reg_v1 WHERE series_uuid NOT IN (SELECT id FROM series_reg_v1)")
    suspend fun findBySeriesIdNotExists(): List<ProductRegistration>


}
