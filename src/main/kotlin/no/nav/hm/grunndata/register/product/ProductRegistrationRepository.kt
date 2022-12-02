package no.nav.hm.grunndata.register.product

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.*

@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class ProductRegistrationRepository() : CoroutineCrudRepository<ProductRegistration, UUID> {


}