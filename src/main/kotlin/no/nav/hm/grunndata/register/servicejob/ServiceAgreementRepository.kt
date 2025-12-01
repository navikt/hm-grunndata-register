package no.nav.hm.grunndata.register.servicejob

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface ServiceAgreementRepository : CoroutineCrudRepository<ServiceAgreement, UUID>,
    CoroutineJpaSpecificationExecutor<ServiceAgreement>  {

    suspend fun findByServiceId(serviceId: UUID): List<ServiceAgreement>
    suspend fun findByAgreementId(agreementId: UUID): List<ServiceAgreement>
    suspend fun findByServiceIdAndAgreementId(serviceId: UUID, agreementId: UUID): ServiceAgreement?

}