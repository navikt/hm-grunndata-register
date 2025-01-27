package no.nav.hm.grunndata.register.catalog

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDateTime
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface CatalogImportRepository:  CoroutineCrudRepository<CatalogImport, UUID>,
    CoroutineJpaSpecificationExecutor<CatalogImport> {

        suspend fun findByOrderRef(orderRef: String): List<CatalogImport>

        @Query("""SELECT DISTINCT on (a.hms_art_nr) a.*, c.title as series_title, c.id as series_id, b.product_id from catalog_import_v1 a, product_agreement_reg_v1 b,  series_reg_v1 c  where a.agreement_id=b.agreement_id and a.order_ref=:orderRef and a.hms_art_nr=b.hms_artnr and b.series_uuid=c.id""", nativeQuery = true)
        suspend fun findCatalogSeriesInfoByOrderRef(orderRef: String): List<CatalogSeriesInfo>

        @Query("""SELECT DISTINCT on (a.hms_art_nr) a.*, c.title as series_title, c.id as series_id, b.product_id from catalog_import_v1 a, product_agreement_reg_v1 b,  series_reg_v1 c  where a.agreement_id=b.agreement_id and a.hms_art_nr=:hmsArtNr and b.series_uuid=c.id""", nativeQuery = true)
        suspend fun findCatalogSeriesInfoByHmsNr(hmsArtNr: String): CatalogSeriesInfo?



}

@Introspected
data class CatalogSeriesInfo(
    val hmsArtNr: String,
    val iso: String,
    val orderRef: String,
    val title: String,
    val supplierRef: String,
    val reference: String,
    val postNr: String,
    val mainProduct: Boolean,
    val sparePart: Boolean,
    val accessory: Boolean,
    val seriesTitle: String,
    val seriesId: UUID,
    val productId: UUID?,
    val agreementId: UUID,
    val created: LocalDateTime,
    val updated: LocalDateTime
)