package no.nav.hm.grunndata.register.product.attributes.produkttype

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import no.nav.hm.grunndata.rapid.dto.ProduktTypeStatus
import java.util.*

@JdbcRepository(dialect = Dialect.POSTGRES)
interface ProduktTypeRegistrationRepository: CoroutineCrudRepository<ProduktTypeRegistration, UUID> {
    suspend fun findByKategoriAndPostId(sortimentKategori: String, postId: UUID): ProduktTypeRegistration?
    suspend fun findByStatus(status: ProduktTypeStatus): List<ProduktTypeRegistration>
}
