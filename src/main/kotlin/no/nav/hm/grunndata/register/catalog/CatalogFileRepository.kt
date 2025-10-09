package no.nav.hm.grunndata.register.catalog

import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.Slice
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.repository.jpa.criteria.QuerySpecification
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDateTime
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.CatalogFileStatus


@JdbcRepository(dialect = Dialect.POSTGRES)
interface CatalogFileRepository: CoroutineCrudRepository<CatalogFile, UUID>, CoroutineJpaSpecificationExecutor<CatalogFile> {

    suspend fun findOne(id: UUID): CatalogFileDTO?

    @Query(
        value = "SELECT id, file_name, file_size, order_ref, supplier_id, updated_by_user, created, updated, status, connected, error_message FROM catalog_file_v1",
        readOnly = true,
        nativeQuery = true
    )
    suspend fun findMany(pageable: Pageable): Slice<CatalogFileDTO>

    @Query(
        value = "SELECT id, file_name, file_size, order_ref, supplier_id, updated_by_user, created, updated, status, connected, error_message FROM catalog_file_v1 where status = :status",
        readOnly = true,
        nativeQuery = true
    )
    suspend fun findManyByStatus(status: CatalogFileStatus, pageable: Pageable): Slice<CatalogFileDTO>

    suspend fun findOneByStatusOrderByCreatedAsc(status: CatalogFileStatus): CatalogFile?

    suspend fun findByStatus(status: CatalogFileStatus): List<CatalogFile>

    suspend fun findByConnectedAndStatus(connected: Boolean, status: CatalogFileStatus): List<CatalogFileDTO>

    suspend fun updatedConnectedUpdatedById(id: UUID, connected: Boolean, updated: LocalDateTime)

    suspend fun findByOrderRef(orderRef: String): List<CatalogFile>

}