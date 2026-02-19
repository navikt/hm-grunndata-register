package no.nav.hm.grunndata.register.catalog

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.jpa.criteria.impl.expression.LiteralExpression
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.http.annotation.*
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.rapid.dto.CatalogFileStatus
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.productagreement.CatalogImportResultReport
import no.nav.hm.grunndata.register.runtime.where
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.userId
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*


@Secured(Roles.ROLE_ADMIN)
@Controller
@Tag(name = "Admin Catalog File")
open class CatalogFileAdminController(
    private val supplierRegistrationService: SupplierRegistrationService,
    private val catalogExcelFileImport: CatalogExcelFileImport,
    private val catalogImportService: CatalogImportService,
    private val catalogFileRepository: CatalogFileRepository
) {

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(CatalogFileAdminController::class.java)
        const val ADMIN_API_V1_CATALOG_FILE = "/admin/api/v1/catalog-file"
    }

    @Post(
        value = "/admin/api/v1/product-agreement/excel-import", // old path kept for backward compatibility
        consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA],
        produces = [io.micronaut.http.MediaType.APPLICATION_JSON],
    )
    suspend fun excelImportBackwardCompatibility(
        file: CompletedFileUpload,
        @QueryValue dryRun: Boolean = true,
        @QueryValue supplierId: UUID,
        authentication: Authentication,
    ): CatalogImportResultReport {
        return excelImport(file, dryRun, supplierId, authentication)
    }

    @Post(
        value = "${ADMIN_API_V1_CATALOG_FILE}/excel-import",
        consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA],
        produces = [io.micronaut.http.MediaType.APPLICATION_JSON],
    )
    suspend fun excelImport(
        file: CompletedFileUpload,
        @QueryValue dryRun: Boolean = true,
        @QueryValue supplierId: UUID,
        authentication: Authentication,
    ): CatalogImportResultReport {
        val supplier = supplierRegistrationService.findById(supplierId)
            ?: throw BadRequestException("Supplier $supplierId not found")
        LOG.info("Importing excel file: ${file.filename}, dryRun: $dryRun by ${authentication.userId()} for supplier ${supplier.name}")
        try {
            val importedExcelCatalog =
                file.inputStream.use { input -> catalogExcelFileImport.importExcelFile(input) }

            val (_, catalogList) = catalogImportService.mapExcelDTOToCatalogImport(
                importedExcelCatalog,
                supplierId,
            )

            val catalogImportResult= catalogImportService.checkForExistingAndMapCatalogImportResult(catalogList, false)
            LOG.info("inserted: ${catalogImportResult.insertedList.size}")
            LOG.info("updated: ${catalogImportResult.updatedList.size}")
            LOG.info("deactivated: ${catalogImportResult.deactivatedList.size}")

            if (!dryRun) {
                LOG.info("Save the catalog file ${file.filename}, for downstream processing")
                catalogFileRepository.save(
                    CatalogFile(
                        fileName = file.filename,
                        fileSize = file.size,
                        orderRef = importedExcelCatalog[0].bestillingsNr,
                        catalogList = importedExcelCatalog,
                        supplierId = supplierId,
                        created = LocalDateTime.now(),
                        updatedByUser = authentication.name,
                        updated = LocalDateTime.now(),
                        status = CatalogFileStatus.PENDING
                    )
                )
            }
            return CatalogImportResultReport(
                supplier = supplier.name,
                file = file.name,
                rows = importedExcelCatalog.size,
                insertedList = catalogImportResult.insertedList,
                updatedList = catalogImportResult.updatedList,
                deactivatedList = catalogImportResult.deactivatedList,
            )
        } catch (e: Exception) {
            LOG.error("Error importing catalog excel file: ${file.filename} for supplier ${supplier.name}", e)
            throw BadRequestException("Feil i catalog fil: ${e.message}")
        }
    }

    @Get("${ADMIN_API_V1_CATALOG_FILE}/")
    suspend fun findCatalogFiles(@RequestBean criteria: CatalogFileCriteria, pageable: Pageable): Page<CatalogFile> =
        catalogFileRepository.findAll(buildCriteriaSpec(criteria), pageable)


    private fun buildCriteriaSpec(
        criteria: CatalogFileCriteria,
    ): PredicateSpecification<CatalogFile>? =
        if (criteria.isNotEmpty()) {
            where {
                criteria.orderRef?.let { root[CatalogFile::orderRef] eq it }
                criteria.supplierId?.let { root[CatalogFile::supplierId] eq it }
                criteria.status?.let { root[CatalogFile::status] eq it }
                criteria.fileName?.let { root[CatalogFile::fileName] like LiteralExpression("%${it}%") }
            }
        } else null

    @Delete("${ADMIN_API_V1_CATALOG_FILE}/{id}")
    suspend fun deleteCatalogFile(id: UUID, authentication: Authentication): Boolean {
        val existing = catalogFileRepository.findById(id) ?: return false
        LOG.info("Deleting catalog file id: $id, name: ${existing.fileName} by user: ${authentication.userId()}")
        catalogFileRepository.delete(existing)
        return true
    }

    @Put("${ADMIN_API_V1_CATALOG_FILE}/{id}/retry")
    suspend fun retryCatalogFile(id: UUID, authentication: Authentication): CatalogFile? {
        val existing = catalogFileRepository.findById(id) ?: return null
        return if (existing.status == CatalogFileStatus.ERROR) {
            val updated = existing.copy(
                status = CatalogFileStatus.PENDING,
                errorMessage = null,
                updated = LocalDateTime.now(),
                updatedByUser = authentication.name
            )
            catalogFileRepository.update(updated)
        } else {
            throw BadRequestException("Can only retry catalog files with status=ERROR")
        }
    }
}

@Introspected
data class CatalogFileCriteria(
    val fileName: String? = null,
    val orderRef: String? = null,
    val supplierId: UUID? = null,
    val status: CatalogFileStatus? = null
) {
    fun isNotEmpty() = fileName != null || orderRef != null || supplierId != null || status != null
}
