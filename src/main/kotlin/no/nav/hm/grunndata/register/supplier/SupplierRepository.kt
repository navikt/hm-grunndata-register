package no.nav.hm.grunndata.register.supplier

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface SupplierRepository :
    CoroutineCrudRepository<SupplierRegistration, UUID>,
    CoroutineJpaSpecificationExecutor<SupplierRegistration> {
    suspend fun findByName(name: String): SupplierRegistration?

    suspend fun findNameAndId(): List<SupplierNameAndId>

    suspend fun findByIdentifier(idenifier: String): SupplierRegistration?
}

@Introspected
data class SupplierNameAndId(val name: String, val id: UUID)
