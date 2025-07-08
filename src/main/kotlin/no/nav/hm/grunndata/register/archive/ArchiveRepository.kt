package no.nav.hm.grunndata.register.archive

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface ArchiveRepository: CoroutineCrudRepository<Archive, UUID> {

    suspend fun findByOid(oid: UUID): List<Archive>
}