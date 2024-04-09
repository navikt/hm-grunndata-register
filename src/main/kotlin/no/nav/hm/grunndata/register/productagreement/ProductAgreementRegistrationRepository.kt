package no.nav.hm.grunndata.register.productagreement

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
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

    suspend fun findBySupplierIdAndSupplierRefAndAgreementIdAndPostAndRankAndStatus(
        supplierId: UUID,
        supplierRef: String,
        agreementId: UUID,
        post: Int,
        rank: Int,
        status: ProductAgreementStatus
    ): ProductAgreementRegistration?

    suspend fun findBySupplierIdAndSupplierRef(
        supplierId: UUID,
        supplierRef: String
    ): List<ProductAgreementRegistration>


    suspend fun findByAgreementId(agreementId: UUID): List<ProductAgreementRegistration>

    suspend fun findByPostId(postId: UUID): List<ProductAgreementRegistration>

    suspend fun findByPostIdAndStatus(postId: UUID, status: ProductAgreementStatus): List<ProductAgreementRegistration>

    suspend fun findAllByIdIn(ids: List<UUID>): List<ProductAgreementRegistration>

    suspend fun findByProductIdIsNull(): List<ProductAgreementRegistration>

    suspend fun findByAgreementIdAndStatus(
        agreementId: UUID,
        status: ProductAgreementStatus
    ): List<ProductAgreementRegistration>
    suspend fun findBySupplierIdAndSupplierRefAndAgreementIdAndPostIdAndRank(
        supplierId: UUID,
        supplierRef: String,
        agreementId: UUID,
        postId: UUID,
        rank: Int,
    ): ProductAgreementRegistration?

}