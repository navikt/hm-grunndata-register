package no.nav.hm.grunndata.register.techlabel

import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository

import java.util.*

@JdbcRepository(dialect = Dialect.POSTGRES)
interface TechLabelRegistrationRepository: CoroutineCrudRepository<TechLabelRegistration, UUID>, CoroutineJpaSpecificationExecutor<TechLabelRegistration> {
    suspend fun findByLabelAndIsoCode(label: String, isoCode: String): TechLabelRegistration?
    suspend fun findByIsoCode(isoCode: String):  List<TechLabelRegistration>
    suspend fun findByLabel(label: String):  List<TechLabelRegistration>
    @Query("SELECT DISTINCT unit FROM techlabel_reg_v1 WHERE unit IS NOT NULL ORDER BY unit")
    suspend fun findDistinctUnits(): List<String>
    @Query("SELECT DISTINCT label FROM techlabel_reg_v1 WHERE label IS NOT NULL ORDER BY label")
    suspend fun findDistinctLabelNames(): List<String>
}