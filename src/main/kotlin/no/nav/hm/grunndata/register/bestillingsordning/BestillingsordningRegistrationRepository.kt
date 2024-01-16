package no.nav.hm.grunndata.register.bestillingsordning

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import no.nav.hm.grunndata.rapid.dto.BestillingsordningStatus
import java.util.*

@JdbcRepository(dialect = Dialect.POSTGRES)
interface BestillingsordningRegistrationRepository: CoroutineCrudRepository<BestillingsordningRegistration, UUID> {

    suspend fun findByHmsArtNr(hmsArtNr: String): BestillingsordningRegistration?

    suspend fun findByStatus(status: BestillingsordningStatus): List<BestillingsordningRegistration>



}