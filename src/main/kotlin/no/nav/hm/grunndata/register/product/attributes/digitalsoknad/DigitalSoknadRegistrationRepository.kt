package no.nav.hm.grunndata.register.product.attributes.digitalsoknad

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import no.nav.hm.grunndata.rapid.dto.DigitalSoknadSortimentStatus
import java.util.*

@JdbcRepository(dialect = Dialect.POSTGRES)
interface DigitalSoknadSortimentRegistrationRepository: CoroutineCrudRepository<DigitalSoknadSortimentRegistration, UUID> {
    suspend fun findBySortimentKategoriAndPostId(sortimentKategori: String, postId: UUID): DigitalSoknadSortimentRegistration?
    suspend fun findByStatus(status: DigitalSoknadSortimentStatus): List<DigitalSoknadSortimentRegistration>
}
