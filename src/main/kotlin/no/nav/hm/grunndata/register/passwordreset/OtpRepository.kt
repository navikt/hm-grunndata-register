package no.nav.hm.grunndata.register.passwordreset

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface OtpRepository : CoroutineCrudRepository<Otp, UUID>, CoroutineJpaSpecificationExecutor<Otp> {
    suspend fun findByOtpAndEmail(
        otp: String,
        email: String,
    ): Otp?


    suspend fun findByEmailAndUsedOrderByCreatedDesc(
        email: String,
        used: Boolean,
    ): Otp?
}
