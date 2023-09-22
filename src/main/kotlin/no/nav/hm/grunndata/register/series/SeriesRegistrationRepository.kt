package no.nav.hm.grunndata.register.series

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.*

@JdbcRepository(dialect = Dialect.POSTGRES)
interface SeriesRegistrationRepository: CoroutineCrudRepository<SeriesRegistration, UUID>, CoroutineJpaSpecificationExecutor<SeriesRegistration> {

    suspend fun findByIdentifier(identifier: String): SeriesRegistration?

}