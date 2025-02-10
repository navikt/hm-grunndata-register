package no.nav.hm.grunndata.register.productagreement

import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import java.time.LocalDateTime
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface ProductAgreementRegistrationRepository :
    CoroutineCrudRepository<ProductAgreementRegistration, UUID>,
    CoroutineJpaSpecificationExecutor<ProductAgreementRegistration> {


    suspend fun findBySupplierIdAndSupplierRef(
        supplierId: UUID,
        supplierRef: String,
    ): List<ProductAgreementRegistration>

    suspend fun findByAgreementId(agreementId: UUID): List<ProductAgreementRegistration>

    suspend fun findByAgreementIdAndStatusAndPublishedBeforeAndExpiredAfter(
        agreementId: UUID,
        status: ProductAgreementStatus,
        published: LocalDateTime,
        expired: LocalDateTime,
    ): List<ProductAgreementRegistration>

    suspend fun findByAgreementIdAndStatusAndExpiredBefore(
        agreementId: UUID,
        status: ProductAgreementStatus,
        expired: LocalDateTime,
    ): List<ProductAgreementRegistration>

    suspend fun findByStatusAndExpiredBefore(
        status: ProductAgreementStatus,
        expired: LocalDateTime,
    ): List<ProductAgreementRegistration>

    @Query(
        """
        SELECT b.* FROM product_reg_v1 a, product_agreement_reg_v1 b WHERE a.expired<now() AND a.draft_status='DONE' 
        AND a.registration_status='INACTIVE' AND b.status='ACTIVE' AND b.product_id=a.id
        """
    )
    suspend fun findProductExpiredButActiveAgreements(): List<ProductAgreementRegistration>

    suspend fun findByPostIdAndStatusIn(
        postId: UUID,
        statuses: List<ProductAgreementStatus>,
    ): List<ProductAgreementRegistration>

    suspend fun findAllByIdIn(ids: List<UUID>): List<ProductAgreementRegistration>

    suspend fun findAllByProductIdIn(ids: List<UUID>): List<ProductAgreementRegistration>

    suspend fun findByAgreementIdAndStatus(
        agreementId: UUID,
        status: ProductAgreementStatus,
    ): List<ProductAgreementRegistration>

    suspend fun findBySupplierIdAndSupplierRefAndAgreementIdAndPostId(
        supplierId: UUID,
        supplierRef: String,
        agreementId: UUID,
        postId: UUID?
    ): ProductAgreementRegistration?

}
