package no.nav.hm.grunndata.register.product

import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.serde.annotation.Serdeable
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import java.time.LocalDateTime
import java.util.PriorityQueue
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

    suspend fun findByHmsArtNrStartingWithAndRegistrationStatusInAndMainProduct(
        hmsArtNr: String,
        registrationStatus: List<RegistrationStatus>,
        mainProduct: Boolean = true,
    ): ProductRegistration?

    suspend fun findByHmsArtNrAndRegistrationStatusIn(
        hmsArtNr: String,
        registrationStatus: List<RegistrationStatus>,
    ): ProductRegistration?


    suspend fun findByHmsArtNrAndRegistrationStatusInAndSupplierId(
        hmsArtNr: String,
        registrationStatus: List<RegistrationStatus>,
        supplierId: UUID,
    ): ProductRegistration?

    @Query("SELECT * FROM product_reg_v1 WHERE product_reg_v1.hms_artnr LIKE :hmsArtNr AND registration_status IN (:registrationStatus) AND (accessory = true OR spare_part = true)")
    suspend fun findPartByHmsArtNrStartingWithAndRegistrationStatusIn(
        hmsArtNr: String,
        registrationStatus: List<RegistrationStatus>,
    ): ProductRegistration?

    suspend fun findByHmsArtNrStartingWithAndRegistrationStatusInAndSupplierIdAndMainProduct(
        hmsArtNr: String,
        registrationStatus: List<RegistrationStatus>,
        supplierId: UUID,
        mainProduct: Boolean = true,
    ): ProductRegistration?

    @Query("SELECT * FROM product_reg_v1 WHERE product_reg_v1.hms_artnr LIKE :hmsArtNr AND registration_status IN (:registrationStatus) AND supplier_id = :supplierId AND (accessory = true OR spare_part = true)")
    suspend fun findPartByHmsArtNrStartingWithAndRegistrationStatusInAndSupplierId(
        hmsArtNr: String,
        registrationStatus: List<RegistrationStatus>,
        supplierId: UUID,
    ): ProductRegistration?

    suspend fun findBySupplierRefAndRegistrationStatusIn(
        supplierRef: String,
        registrationStatus: List<RegistrationStatus>,
    ): ProductRegistration?

    @Query("SELECT * FROM product_reg_v1 WHERE product_reg_v1.supplier_ref LIKE :supplierRef AND registration_status IN (:registrationStatus) AND (accessory = true OR spare_part = true)")
    suspend fun findPartBySupplierRefAndRegistrationStatusIn(
        supplierRef: String,
        registrationStatus: List<RegistrationStatus>,
    ): ProductRegistration?

    suspend fun findBySupplierRefStartingWithAndRegistrationStatusInAndSupplierIdAndMainProduct(
        supplierRef: String,
        registrationStatus: List<RegistrationStatus>,
        supplierId: UUID,
        mainProduct: Boolean = true,
    ): ProductRegistration?

    @Query("SELECT * FROM product_reg_v1 WHERE product_reg_v1.supplier_ref LIKE :supplierRef AND registration_status IN (:registrationStatus) AND supplier_id = :supplierId AND (accessory = true OR spare_part = true)")
    suspend fun findPartBySupplierRefStartingWithAndRegistrationStatusInAndSupplierId(
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

    suspend fun findAllBySeriesUUIDOrderByCreatedAsc(seriesUUID: UUID): List<ProductRegistration>

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

    @Query("select distinct on (id) * from product_reg_v1 WHERE iso_category = :isoCategory AND (product_data->'techData') @> CAST(:jsonQuery as jsonb)", nativeQuery = true)
    suspend fun findDistinctByProductIsoCategoryAndTechDataJsonQuery(isoCategory: String, jsonQuery: String): List<ProductRegistration>

    @Query("SELECT distinct on (a.id) a.* from product_reg_v1 a, product_agreement_reg_v1 b where a.id=b.product_id and (a.main_product != b.main_product or a.spare_part != b.spare_part or a.accessory != b.accessory) and b.status='ACTIVE'")
    suspend fun findProductThatDoesNotMatchAgreementSparePartAccessory(): List<ProductRegistration>

    @Query("SELECT distinct on(a.id) a.* from product_reg_v1 a, product_agreement_reg_v1 b where a.id=b.product_id and b.status='ACTIVE' and a.hms_art_nr != b.hms_art_nr")
    suspend fun findProductThatDoesNotMatchAgreementHmsNr(): List<ProductRegistration>

    @Query("SELECT * from product_reg_v1 where (accessory = true or spare_part = true) and product_data->'attributes'->'compatibleWith' is null")
    suspend fun findAccessoryOrSparePartButNoCompatibleWith(): List<ProductRegistration>

    @Query("SELECT * FROM product_reg_v1 WHERE (accessory = true OR spare_part = true) AND product_data->'attributes'->'compatibleWith'->'seriesIds' @> to_jsonb(:seriesUUID::text) ORDER BY hms_artnr")
    suspend fun findAccessoryOrSparePartCombatibleWithSeriesId(seriesUUID: UUID): List<ProductRegistration>

    @Query("SELECT * FROM product_reg_v1 WHERE (accessory = true OR spare_part = true) AND supplier_id = :supplierId AND product_data->'attributes'->'compatibleWith'->'seriesIds' @> to_jsonb(:seriesUUID::text) ORDER BY hms_artnr")
    suspend fun findAccessoryOrSparePartCombatibleWithSeriesIdAndSupplierId(seriesUUID: UUID, supplierId: UUID): List<ProductRegistration>

    @Query("SELECT distinct on(a.id) a.* from product_reg_v1 a, product_agreement_reg_v1 b where a.id=b.product_id and b.status='ACTIVE' and a.registration_status='INACTIVE'")
    suspend fun findProductThatDoesNotMatchAgreementStatus(): List<ProductRegistration>

    @Query("SELECT array_agg(id) as ids, series_uuid, count(*) FROM product_reg_v1 WHERE main_product = false group by series_uuid having count(*) > 1")
    suspend fun findNotMainProductsThatIsInSeries():List<ProductIdSeriesUUID>

    @Query("""SELECT id, hms_artnr, created FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY hms_artnr ORDER BY created ASC) AS rn, COUNT(*) OVER (PARTITION BY hms_artnr) AS cnt FROM product_reg_v1 WHERE hms_artnr IS NOT NULL) p WHERE cnt > 1 AND rn = 1""")
    suspend fun findAllDuplicateHmsArtnr(
    ): List<ProductIdHmsArtNr>

    suspend fun findByRegistrationStatus(status: RegistrationStatus): List<ProductRegistration>

}

@Serdeable
data class ProductIdHmsArtNr (
    val id: UUID,
    val hmsArtNr: String,
    val created: LocalDateTime,
)

@Serdeable
data class ProductIdSeriesUUID (
    val ids: List<UUID>,
    val seriesUUID: UUID,
    val count: Int,
)