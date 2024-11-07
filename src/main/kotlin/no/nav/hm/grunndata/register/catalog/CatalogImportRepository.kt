package no.nav.hm.grunndata.register.catalog

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface CatalogImportRepository:  CoroutineCrudRepository<CatalogImport, UUID>,
    CoroutineJpaSpecificationExecutor<CatalogImport> {

        suspend fun findByOrderRef(orderRef: String): List<CatalogImport>

}