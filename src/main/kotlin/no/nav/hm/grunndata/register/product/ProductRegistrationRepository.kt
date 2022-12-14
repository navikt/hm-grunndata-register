package no.nav.hm.grunndata.register.product

import com.fasterxml.jackson.databind.deser.std.UUIDDeserializer
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import kotlinx.coroutines.flow.Flow
import java.util.*

@JdbcRepository(dialect = Dialect.POSTGRES)
interface ProductRegistrationRepository : CoroutineCrudRepository<ProductRegistration, UUID> {

    suspend fun findByIdAndSupplierId(id:UUID, supplierId: UUID): ProductRegistration?

    suspend fun findBySupplierId(supplierId: UUID, pageable: Pageable): Page<ProductRegistration>

}