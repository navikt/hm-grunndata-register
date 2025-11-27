package no.nav.hm.grunndata.register.serviceoffering

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface ServiceOfferingRepository : CoroutineCrudRepository<ServiceOffering, UUID>,
    CoroutineJpaSpecificationExecutor<ServiceOffering> {

        suspend fun findBySupplierIdAndHmsArtNr(supplierId: UUID, hmsArtNr: String): ServiceOffering?
    }