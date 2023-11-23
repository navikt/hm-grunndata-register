package no.nav.hm.grunndata.register.agreement

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDateTime
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface AgreementRegistrationRepository: CoroutineCrudRepository<AgreementRegistration, UUID>,
    CoroutineJpaSpecificationExecutor<AgreementRegistration> {

        suspend fun findByReference(reference: String): AgreementRegistration?

        suspend fun find(): List<AgreementPDTO>
}

@Introspected
data class AgreementPDTO(val title:String,
                         val reference: String,
                         val id: UUID,
                         val published: LocalDateTime,
                         val expired: LocalDateTime)
