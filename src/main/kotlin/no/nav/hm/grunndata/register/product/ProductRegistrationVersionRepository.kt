package no.nav.hm.grunndata.register.product

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDateTime
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus


@JdbcRepository(dialect = Dialect.POSTGRES)
interface ProductRegistrationVersionRepository:  CoroutineCrudRepository<ProductRegistrationVersion, UUID>,
    CoroutineJpaSpecificationExecutor<ProductRegistrationVersion> {

    suspend fun findOneByProductIdAndDraftStatusAndAdminStatusOrderByUpdatedDesc(
        productId: UUID,
        draftStatus: DraftStatus,
        adminStatus: AdminStatus
    ): ProductRegistrationVersion?

    suspend fun findByDraftStatusAndUpdatedBefore(
        draftStatus: DraftStatus,
        minusMonths: LocalDateTime?
    ): List<ProductRegistrationVersion>

    suspend  fun findByProductIdAndVersion(productId: UUID, version: Long): ProductRegistrationVersion?
}
