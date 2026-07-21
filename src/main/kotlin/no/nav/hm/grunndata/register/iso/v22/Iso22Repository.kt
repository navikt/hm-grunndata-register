package no.nav.hm.grunndata.register.iso.v22

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID


@JdbcRepository(dialect = Dialect.POSTGRES)
interface Iso22Repository : CoroutineCrudRepository<Iso22, UUID> {

    @Query("SELECT DISTINCT iso_category FROM series_reg_v1 WHERE iso_category IS NOT NULL")
    suspend fun findDistinctIsoCodes(): List<SeriesIsoCategory>

    suspend fun findByIsoCode(isoCode: String): Iso22?



}

@Introspected
data class SeriesIsoCategory(
    val isoCategory: String
)