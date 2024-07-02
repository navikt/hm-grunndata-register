package no.nav.hm.grunndata.register.product.attributes.paakrevdgodkjenningskurs

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import no.nav.hm.grunndata.rapid.dto.PaakrevdGodkjenningskursStatus
import java.util.*

@JdbcRepository(dialect = Dialect.POSTGRES)
interface PaakrevdGodkjenningskursRegistrationRepository: CoroutineCrudRepository<PaakrevdGodkjenningskursRegistration, UUID> {
    suspend fun findByIsokode(isokode: String): PaakrevdGodkjenningskursRegistration?
    suspend fun findByStatus(status: PaakrevdGodkjenningskursStatus): List<PaakrevdGodkjenningskursRegistration>
}
