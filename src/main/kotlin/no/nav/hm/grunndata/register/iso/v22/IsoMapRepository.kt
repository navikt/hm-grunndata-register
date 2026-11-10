package no.nav.hm.grunndata.register.iso.v22

import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.*

@JdbcRepository(dialect = Dialect.POSTGRES)
interface IsoMapRepository : CoroutineCrudRepository<IsoMap, UUID> {
    @CacheInvalidate(cacheNames = ["isomap-all"], all = true)
    override suspend fun <S : IsoMap> save(entity: S): S

    @CacheInvalidate(cacheNames = ["isomap-all"], all = true)
    override suspend fun <S : IsoMap> update(entity: S): S

    suspend fun findByCode16AndCode22(code16: String, code22: String): IsoMap?
    suspend fun countByVerified(verified: Boolean): Long
    suspend fun countVerified(): Long = countByVerified(true)
}