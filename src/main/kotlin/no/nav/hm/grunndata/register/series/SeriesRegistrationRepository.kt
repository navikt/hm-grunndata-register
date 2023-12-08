package no.nav.hm.grunndata.register.series

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
interface SeriesRegistrationRepository: CoroutineCrudRepository<SeriesRegistration, UUID>, CoroutineJpaSpecificationExecutor<SeriesRegistration> {

    suspend fun findByIdentifier(identifier: String): SeriesRegistration?

    @Query(value="select title, series_id,count(*) from product_reg_v1 WHERE supplier_id = :supplierId group by (title, series_id) ORDER BY title asc", readOnly = true, nativeQuery = true)
    suspend fun findSeriesGroup(supplierId: UUID, pageable: Pageable): Slice<SeriesGroupDTO>

    @Query(value="select title, series_id,count(*) from product_reg_v1 group by (title, series_id) ORDER BY title asc", readOnly = true, nativeQuery = true)
    suspend fun findSeriesGroup(pageable: Pageable): Slice<SeriesGroupDTO>

}


@Introspected
data class SeriesGroupDTO(
    val title: String,
    val seriesId: String,
    val count: Long
)