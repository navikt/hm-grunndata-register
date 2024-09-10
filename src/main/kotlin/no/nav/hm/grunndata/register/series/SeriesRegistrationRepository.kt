package no.nav.hm.grunndata.register.series

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.Slice
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface SeriesRegistrationRepository :
    CoroutineCrudRepository<SeriesRegistration, UUID>,
    CoroutineJpaSpecificationExecutor<SeriesRegistration> {
    @Query(
        value = "select title, series_id,count(*) from product_reg_v1 WHERE supplier_id = :supplierId group by (title, series_id) ORDER BY title asc",
        readOnly = true,
        nativeQuery = true,
    )
    suspend fun findSeriesGroup(
        supplierId: UUID,
        pageable: Pageable,
    ): Slice<SeriesGroupDTO>

    @Query(
        value = "select title, series_id,count(*) from product_reg_v1 group by (title, series_id) ORDER BY title asc",
        readOnly = true,
        nativeQuery = true,
    )
    suspend fun findSeriesGroup(pageable: Pageable): Slice<SeriesGroupDTO>

    @Query("SELECT a.* from series_reg_v1 a LEFT JOIN product_reg_v1 b on a.id = b.series_uuid where b.series_uuid is null")
    suspend fun findSeriesThatDoesNotHaveProducts(): List<SeriesRegistration>

    suspend fun findBySupplierId(supplierId: UUID): List<SeriesRegistration>

    suspend fun findByIdIn(ids: List<UUID>): List<SeriesRegistration>

    suspend fun findByIdAndStatusIn(id: UUID, statuses: List<SeriesStatus>): SeriesRegistration?

    suspend fun findByIdAndSupplierId(
        id: UUID,
        supplierId: UUID,
    ): SeriesRegistration?

    suspend fun findByIdAndSupplierIdAndStatusIn(
        id: UUID,
        supplierId: UUID,
        statuses: List<SeriesStatus>,
    ): SeriesRegistration?

    @Query(
        "UPDATE series_reg_v1 SET count = b.b_count FROM (SELECT series_uuid, count(*) as b_count FROM product_reg_v1 GROUP BY series_uuid) AS b WHERE id = b.series_uuid AND b.series_uuid = :id",
    )
    suspend fun updateCountForSeries(id: UUID)

    @Query(
        "UPDATE series_reg_v1 " +
            "SET count_drafts = b.b_count " +
            "FROM (SELECT series_uuid, count(*) as b_count FROM product_reg_v1 WHERE draft_status = 'DRAFT' AND registration_status = 'ACTIVE' AND admin_status != 'REJECTED' GROUP BY series_uuid) AS b " +
            "WHERE id = b.series_uuid AND b.series_uuid = :id",
    )
    suspend fun updateCountDraftsForSeries(id: UUID)

    @Query(
        "UPDATE series_reg_v1 " +
            "SET count_published = b.b_count " +
            "FROM (SELECT series_uuid, count(*) as b_count FROM product_reg_v1 WHERE draft_status = 'DONE' AND registration_status = 'ACTIVE' AND admin_status = 'APPROVED' GROUP BY series_uuid) AS b " +
            "WHERE id = b.series_uuid AND b.series_uuid = :id",
    )
    suspend fun updateCountPublishedForSeries(id: UUID)

    @Query(
        "UPDATE series_reg_v1 " +
            "SET count_pending = b.b_count " +
            "FROM (SELECT series_uuid, count(*) as b_count FROM product_reg_v1 WHERE draft_status = 'DONE' AND registration_status = 'ACTIVE' AND admin_status = 'PENDING' GROUP BY series_uuid) AS b " +
            "WHERE id = b.series_uuid AND b.series_uuid = :id",
    )
    suspend fun updateCountPendingForSeries(id: UUID)

    @Query(
        "UPDATE series_reg_v1 " +
            "SET count_declined = b.b_count " +
            "FROM (SELECT series_uuid, count(*) as b_count FROM product_reg_v1 WHERE draft_status = 'DRAFT' AND registration_status = 'ACTIVE' AND admin_status = 'REJECTED' GROUP BY series_uuid) AS b " +
            "WHERE id = b.series_uuid AND b.series_uuid = :id",
    )
    suspend fun updateCountDeclinedForSeries(id: UUID)

    @Query(
        "UPDATE series_reg_v1 SET count_drafts = 0, count_published = 0, count_pending=0, count_declined=0 WHERE id = :id",
    )
    suspend fun resetCountStatusesForSeries(id: UUID)

    suspend fun findByIsoCategory(isoCategory: String): List<SeriesRegistration>

    @Query(
        "UPDATE series_reg_v1 SET status = 'INACTIVE' WHERE id = :id AND NOT EXISTS( SELECT 1 FROM product_reg_v1 WHERE series_uuid = :id AND registration_status = 'ACTIVE')",
    )
    suspend fun updateStatusForSeries(id: UUID)

    @Query(
        "UPDATE series_reg_v1 SET status = :newStatus WHERE id = :id AND NOT EXISTS( SELECT 1 FROM product_reg_v1 WHERE series_uuid = :id AND registration_status = 'ACTIVE')",
    )
    suspend fun updateStatusForSeries(
        id: UUID,
        newStatus: String,
    )
}

@Introspected
data class SeriesGroupDTO(
    val title: String,
    val seriesId: String,
    val count: Long,
)
