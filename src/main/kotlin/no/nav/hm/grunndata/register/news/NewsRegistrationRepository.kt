package no.nav.hm.grunndata.register.news

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import no.nav.hm.grunndata.rapid.dto.NewsStatus
import java.util.*

@JdbcRepository(dialect = Dialect.POSTGRES)
interface NewsRegistrationRepository: CoroutineCrudRepository<NewsRegistration, UUID> {
    suspend fun findByStatus(status: NewsStatus): List<NewsRegistration>

}
