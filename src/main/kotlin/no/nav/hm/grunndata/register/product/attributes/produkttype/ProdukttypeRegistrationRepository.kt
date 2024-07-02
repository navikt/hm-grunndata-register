package no.nav.hm.grunndata.register.product.attributes.produkttype

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import no.nav.hm.grunndata.rapid.dto.ProdukttypeStatus
import java.util.*

@JdbcRepository(dialect = Dialect.POSTGRES)
interface ProdukttypeRegistrationRepository: CoroutineCrudRepository<ProdukttypeRegistration, UUID> {
    suspend fun findByIsokode(isokode: String): ProdukttypeRegistration?
    suspend fun findByStatus(status: ProdukttypeStatus): List<ProdukttypeRegistration>
}
