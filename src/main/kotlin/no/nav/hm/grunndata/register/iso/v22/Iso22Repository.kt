package no.nav.hm.grunndata.register.iso.v22

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID


@JdbcRepository(dialect = Dialect.POSTGRES)
interface Iso22Repository : CoroutineCrudRepository<Iso22, UUID> {

    suspend fun findByIsoCode(isoCode: String): Iso22?



}
