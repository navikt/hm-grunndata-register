package no.nav.hm.grunndata.register.iso.v22

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.*

@JdbcRepository(dialect = Dialect.POSTGRES)
interface IsoMapRepository : CoroutineCrudRepository<IsoMap, UUID> {
    suspend fun findByCode16(code16: String): IsoMap?
    suspend fun findByCode16AndCode22(code16: String, code22: String): IsoMap?
}