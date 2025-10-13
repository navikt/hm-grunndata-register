package no.nav.hm.grunndata.register.part.hidden

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface HiddenPartRepository: CoroutineCrudRepository<HiddenPart, UUID> {
    suspend fun findByProductId(productId: UUID): HiddenPart?
    suspend fun existsByProductId(productId: UUID): Boolean
}

