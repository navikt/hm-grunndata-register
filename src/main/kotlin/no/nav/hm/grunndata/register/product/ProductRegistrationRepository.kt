package no.nav.hm.grunndata.register.product

import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
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

    suspend fun findByHmsArtNrAndSupplierId(
        hmsArtNr: String,
        supplierId: UUID,
    ): ProductRegistration?

    suspend fun findByHmsArtNr(hmsArtNr: String): ProductRegistration?

    suspend fun findBySupplierRefAndSupplierId(
        supplierRef: String,
        supplierId: UUID,
    ): ProductRegistration?

    suspend fun existsBySupplierRefAndSupplierId(
        supplierRef: String,
        supplierId: UUID,
    ): Boolean

    suspend fun existsBySeriesUUIDAndArticleName(
        seriesUUID: UUID,
        articleName: String,
    ): Boolean

    suspend fun findBySupplierId(supplierId: UUID): List<ProductRegistration>

    suspend fun findBySeriesUUIDAndSupplierId(
        seriesUUID: UUID,
        supplierId: UUID,
    ): List<ProductRegistration>

    @Query("SELECT a.* from product_reg_v1 a LEFT JOIN series_reg_v1 b on a.series_uuid = b.id where b.id is null")
    suspend fun findProductsWithNoSeries(): List<ProductRegistration>

    suspend fun findByRegistrationStatus(registrationStatus: RegistrationStatus): List<ProductRegistration>

    suspend fun findAllBySeriesUUID(seriesUUID: UUID): List<ProductRegistration>

    suspend fun findBySeriesUUID(seriesUUID: UUID): ProductRegistration?
}
