package no.nav.hm.grunndata.register.product

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.Slice
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.*

@JdbcRepository(dialect = Dialect.POSTGRES)
interface ProductRegistrationRepository : CoroutineCrudRepository<ProductRegistration, UUID>,
    CoroutineJpaSpecificationExecutor<ProductRegistration> {

    suspend fun findByIdAndSupplierId(id:UUID, supplierId: UUID): ProductRegistration?

    suspend fun findByHmsArtNrAndSupplierId(hmsArtNr: String, supplierId: UUID): ProductRegistration?

    suspend fun findBySupplierRefAndSupplierId(supplierRef: String, supplierId: UUID): ProductRegistration?

    suspend fun findBySeriesIdAndSupplierId(seriesId: String, supplierId: UUID): List<ProductRegistration>

    @Query("select title, series_id,count(*) from product_reg_v1 WHERE supplier_id= :supplierId group by (title, series_id)")
    suspend fun findSeriesGroup(supplierId: UUID, pageable: Pageable): Slice<SeriesGroupDTO>

    @Query("select title, series_id,count(*) from product_reg_v1 group by (title, series_id)")
    suspend fun findSeriesGroup(pageable: Pageable): Slice<SeriesGroupDTO>
    suspend fun findBySeriesId(seriesId: String): List<ProductRegistration>

}

@Introspected
data class SeriesGroupDTO(
    val title: String,
    val seriesId: String,
    val count: Long
)