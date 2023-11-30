package no.nav.hm.grunndata.register.productagreement

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.*

@JdbcRepository(dialect = Dialect.POSTGRES)
interface ProductAgreementRegistrationRepository : CoroutineCrudRepository<ProductAgreementRegistration, UUID>,
    CoroutineJpaSpecificationExecutor<ProductAgreementRegistration> {

    suspend fun findBySupplierIdAndSupplierRefAndAgreementIdAndPostAndRank(
        supplierId: UUID,
        supplierRef: String,
        agreementId: UUID,
        post: Int,
        rank: Int
    ): ProductAgreementRegistration?

    suspend fun findBySupplierIdAndSupplierRef(
        supplierId: UUID,
        supplierRef: String
    ): List<ProductAgreementRegistration>


}