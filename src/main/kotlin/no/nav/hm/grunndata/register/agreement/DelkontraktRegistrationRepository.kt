package no.nav.hm.grunndata.register.agreement

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface DelkontraktRegistrationRepository: CoroutineCrudRepository<DelkontraktRegistration, UUID> {

    suspend fun findByIdentifier(identifier: String): DelkontraktRegistration?
    suspend fun findByAgreementId(agreementId: UUID): List<DelkontraktRegistration>
    suspend fun findByAgreementIdAndType(agreementId: UUID, type: DelkontraktType): DelkontraktRegistration?

}