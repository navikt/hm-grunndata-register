package no.nav.hm.grunndata.register.agreement

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface AgreementRegistrationRepository: CoroutineCrudRepository<AgreementRegistration, UUID>,
    CoroutineJpaSpecificationExecutor<AgreementRegistration> {

        fun findByReference(reference: String): AgreementRegistration?
}