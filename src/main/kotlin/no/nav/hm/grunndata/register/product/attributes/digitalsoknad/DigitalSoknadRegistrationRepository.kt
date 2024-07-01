package no.nav.hm.grunndata.register.product.attributes.digitalsoknad

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import no.nav.hm.grunndata.rapid.dto.DigitalSoknadStatus
import java.util.*

@JdbcRepository(dialect = Dialect.POSTGRES)
interface DigitalSoknadRegistrationRepository: CoroutineCrudRepository<DigitalSoknadRegistration, UUID> {
    suspend fun findByHmsArtNr(hmsArtNr: String): DigitalSoknadRegistration?
    suspend fun findByStatus(status: DigitalSoknadStatus): List<DigitalSoknadRegistration>
}
