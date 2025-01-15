package no.nav.hm.grunndata.register.product

import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import java.time.LocalDateTime
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface ProductRegistrationRepository :
    CoroutineCrudRepository<ProductRegistration, UUID>,
    CoroutineJpaSpecificationExecutor<ProductRegistration> {
    suspend fun findByIdAndSupplierId(
        id: UUID,
        supplierId: UUID,
    ): ProductRegistration?

    suspend fun findByIdIn(ids: List<UUID>): List<ProductRegistration>

    suspend fun findByHmsArtNrOrSupplierRef(
        hmsArtNr: String,
        supplierRef: String,
    ): List<ProductRegistration>

    suspend fun findByHmsArtNrAndSupplierId(
        hmsArtNr: String,
        supplierId: UUID,
    ): ProductRegistration?

    suspend fun findByHmsArtNrStartingWithAndRegistrationStatusIn(
        hmsArtNr: String,
        registrationStatus: List<RegistrationStatus>,
    ): ProductRegistration?

    suspend fun findByHmsArtNrStartingWithAndRegistrationStatusInAndSupplierId(
        hmsArtNr: String,
        registrationStatus: List<RegistrationStatus>,
        supplierId: UUID,
    ): ProductRegistration?

    suspend fun findBySupplierRefAndRegistrationStatusIn(
        supplierRef: String,
        registrationStatus: List<RegistrationStatus>,
    ): ProductRegistration?

    suspend fun findBySupplierRefStartingWithAndRegistrationStatusInAndSupplierId(
        supplierRef: String,
        registrationStatus: List<RegistrationStatus>,
        supplierId: UUID
    ): ProductRegistration?

    suspend fun findBySupplierRefAndSupplierId(
        supplierRef: String,
        supplierId: UUID,
    ): ProductRegistration?

    suspend fun existsBySeriesUUIDAndSupplierId(
        seriesUUID: UUID,
        supplierId: UUID,
    ): Boolean

    suspend fun findBySupplierId(supplierId: UUID): List<ProductRegistration>

    suspend fun findBySeriesUUIDAndSupplierId(
        seriesUUID: UUID,
        supplierId: UUID,
    ): List<ProductRegistration>

    @Query("SELECT a.* from product_reg_v1 a LEFT JOIN series_reg_v1 b on a.series_uuid = b.id where b.id is null")
    suspend fun findProductsWithNoSeries(): List<ProductRegistration>

    suspend fun findAllBySeriesUUID(seriesUUID: UUID): List<ProductRegistration>

    suspend fun findAllBySeriesUUIDAndRegistrationStatusAndPublishedIsNotNull(
        seriesUUID: UUID,
        registrationStatus: RegistrationStatus,
    ): List<ProductRegistration>

    suspend fun findAllBySeriesUUIDAndAdminStatus(
        seriesUUID: UUID,
        adminStatus: AdminStatus,
    ): List<ProductRegistration>

    suspend fun findBySeriesUUID(seriesUUID: UUID): ProductRegistration?

    suspend fun findByRegistrationStatusAndExpiredBefore(
        registrationStatus: RegistrationStatus,
        expired: LocalDateTime,
    ): List<ProductRegistration>

    suspend fun findByRegistrationStatusAndAdminStatusAndPublishedBeforeAndExpiredAfter(
        registrationStatus: RegistrationStatus,
        adminStatus: AdminStatus,
        published: LocalDateTime,
        expired: LocalDateTime,
    ): List<ProductRegistration>

    @Query("SELECT a.* from product_reg_v1 a where a.series_uuid = :seriesUUID order by a.created desc limit 1")
    suspend fun findLastProductInSeries(seriesUUID: UUID): ProductRegistration?

    @Query("select distinct on (id) * from product_reg_v1 WHERE (product_data->'techData') @> CAST(:jsonQuery as jsonb)", nativeQuery = true)
    suspend fun findDistinctByProductTechDataJsonQuery(jsonQuery: String): List<ProductRegistration>

    @Query("SELECT distinct on (a.id) a.* from product_reg_v1 a, product_agreement_reg_v1 b where a.id=b.product_id and (a.spare_part != b.spare_part or a.accessory != b.accessory)")
    suspend fun findProductThatDoesNotMatchAgreementSparePartAccessory(): List<ProductRegistration>


}
