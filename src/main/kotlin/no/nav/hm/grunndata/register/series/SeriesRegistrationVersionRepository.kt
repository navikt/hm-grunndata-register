package no.nav.hm.grunndata.register.series

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus

@JdbcRepository(dialect = Dialect.POSTGRES)
interface SeriesRegistrationVersionRepository : CoroutineCrudRepository<SeriesRegistrationVersion, UUID>,
    CoroutineJpaSpecificationExecutor<SeriesRegistrationVersion> {

        suspend fun findOneByDraftStatusAndAdminStatusOrderByUpdatedDesc
                    (draftStatus: DraftStatus, adminStatus: AdminStatus): SeriesRegistrationVersion?



}