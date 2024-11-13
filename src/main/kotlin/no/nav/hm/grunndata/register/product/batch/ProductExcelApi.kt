package no.nav.hm.grunndata.register.product.batch

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.http.server.types.files.StreamedFile
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import no.nav.helse.rapids_rivers.toUUID
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.product.ProductDTOMapper
import no.nav.hm.grunndata.register.product.ProductRegistrationDTO
import no.nav.hm.grunndata.register.product.ProductRegistrationDryRunDTO
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.product.batch.ProductExcelApi.Companion.API_V1_VENDOR_PRODUCT_REGISTRATIONS_EXCEL
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.supplierId
import no.nav.hm.grunndata.register.series.SeriesRegistrationService
import org.apache.commons.io.output.ByteArrayOutputStream
import org.slf4j.LoggerFactory


@Secured(Roles.ROLE_SUPPLIER)
@Controller(API_V1_VENDOR_PRODUCT_REGISTRATIONS_EXCEL)
@Tag(name = "Product Excel API")
class ProductExcelApi(
    private val productRegistrationService: ProductRegistrationService,
    private val xlImport: ProductExcelImport,
    private val xlExport: ProductExcelExport,
    private val productDTOMapper: ProductDTOMapper,
    private val excelExportMapper: ExcelExportMapper,
    private val seriesRegistrationService: SeriesRegistrationService
) {
    companion object {
        const val API_V1_VENDOR_PRODUCT_REGISTRATIONS_EXCEL = "/vendor/api/v1/product/registrations/excel"
        private val LOG = LoggerFactory.getLogger(ProductExcelApi::class.java)
    }

    @Post(
        "/export",
        consumes = ["application/json"],
        produces = ["application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"],
    )
    suspend fun createExport(
        @Body uuids: List<UUID>,
        authentication: Authentication,
    ): HttpResponse<StreamedFile> {
        val products = uuids.mapNotNull { productRegistrationService.findById(it) }
        products.forEach {
            if (it.supplierId != authentication.supplierId()) {
                throw BadRequestException("Unauthorized access to product ${it.id}")
            }
        }
        if (products.isEmpty()) throw BadRequestException("No products found")

        return exportExcel(products.map { productDTOMapper.toDTO(it) })
    }

    @Post(
        "/export/supplier",
        consumes = ["application/json"],
        produces = ["application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"],
    )
    suspend fun createExportForAllSupplierProducts(authentication: Authentication): HttpResponse<StreamedFile> {
        val products = productRegistrationService.findBySupplierId(authentication.supplierId())
        if (products.isEmpty()) throw BadRequestException("No products found")

        return exportExcel(products.map { productDTOMapper.toDTO(it) })
    }

    @Post(
        "/export/supplier/{seriesId}",
        consumes = ["application/json"],
        produces = ["application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"],
    )
    suspend fun createExportForSeries(
        @PathVariable seriesId: UUID,
        authentication: Authentication,
    ): HttpResponse<StreamedFile> {
        val products = productRegistrationService.findBySeriesUUIDAndSupplierId(seriesId, authentication.supplierId())
        if (products.isEmpty()) throw BadRequestException("No products found")

        return exportExcel(products.map { productDTOMapper.toDTO(it) })
    }

    private suspend fun exportExcel(products: List<ProductRegistrationDTO>): HttpResponse<StreamedFile> {
        val excelExportDtos = excelExportMapper.mapToExportDtos(products)

        val id = UUID.randomUUID()
        LOG.info("Generating Excel file: $id.xlsx")
        return ByteArrayOutputStream().use { byteStream ->
            xlExport.createWorkbookToOutputStream(excelExportDtos, byteStream)
            HttpResponse.ok(StreamedFile(byteStream.toInputStream(), MediaType.MICROSOFT_EXCEL_OPEN_XML_TYPE))
                .header("Content-Disposition", "attachment; filename=$id.xlsx")
        }
    }

    @Post(
        "/import/{seriesId}",
        consumes = [MediaType.MULTIPART_FORM_DATA],
        produces = [MediaType.APPLICATION_JSON],
    )
    suspend fun importExcelForSeries(
        @PathVariable seriesId: UUID,
        file: CompletedFileUpload,
        authentication: Authentication,
    ): HttpResponse<List<ProductRegistrationDTO>> {
        LOG.info("Importing Excel file ${file.filename} for supplierId ${authentication.supplierId()}")
        return file.inputStream.use { inputStream ->
            val excelDTOList = xlImport.importExcelFileForRegistration(inputStream)
            validateProductsToBeImported(excelDTOList, seriesId, authentication)
            LOG.info("found ${excelDTOList.size} products in Excel file")
            val products = productRegistrationService.importExcelRegistrations(excelDTOList, authentication)
            val seriesToUpdate = seriesRegistrationService.findById(seriesId)
            requireNotNull(seriesToUpdate)
            // todo: could it ever be needed to change unpublished to draft also?
            if (seriesToUpdate.published != null) {
                seriesRegistrationService.setSeriesToDraftStatus(seriesToUpdate, authentication)
            }

            HttpResponse.ok(products.map { productDTOMapper.toDTO(it) })
        }
    }

    @Post(
        "/import-dryrun/{seriesId}",
        consumes = [MediaType.MULTIPART_FORM_DATA],
        produces = [MediaType.APPLICATION_JSON],
    )
    suspend fun importExcelForSeriesDryrun(
        @PathVariable seriesId: UUID,
        file: CompletedFileUpload,
        authentication: Authentication,
    ): HttpResponse<List<ProductRegistrationDryRunDTO>> {
        LOG.info("Dryrun for import of Excel file ${file.filename} for supplierId ${authentication.supplierId()}")
        return file.inputStream.use { inputStream ->
            val excelDTOList = xlImport.importExcelFileForRegistration(inputStream)
            validateProductsToBeImported(excelDTOList, seriesId, authentication)
            LOG.info("found ${excelDTOList.size} products in Excel file")
            val products = productRegistrationService.importDryRunExcelRegistrations(excelDTOList, authentication)
            HttpResponse.ok(products)
        }
    }

    private suspend fun validateProductsToBeImported(
        dtos: List<ProductRegistrationExcelDTO>,
        seriesId: UUID,
        authentication: Authentication,
    ) {
        val levArtNrUniqueList = dtos.map { it.levartnr }.distinct()
        if (levArtNrUniqueList.size < dtos.size) {
            throw BadRequestException("Det finnes produkter med samme lev-artnr. i filen. Disse må være unike.")
        }

        val seriesUniqueList = dtos.map { it.produktserieid.toUUID() }.distinct()

        if (seriesUniqueList.size > 1) {
            throw BadRequestException(
                "Det finnes produkter tilknyttet ulike produktserier i filen. " +
                        "Det er kun støtte for å importere produkter til en produktserie om gangen",
            )
        }

        if (seriesUniqueList.size == 1 && seriesUniqueList[0] != seriesId) {
            throw BadRequestException("Produktserien i filen er ulik produktserien du importerte for.")
        }

        seriesUniqueList.forEach {
            if (!productRegistrationService.exitsBySeriesUUIDAndSupplierId(it, authentication.supplierId())) {
                throw BadRequestException("ProduktserieId $it finnes ikke for leverandør ${authentication.supplierId()}")
            }
        }
        dtos.forEach {
            if (it.leverandorid.toUUID() != authentication.supplierId()) {
                throw BadRequestException(
                    "Innlogget bruker har ikke rettigheter til leverandørId ${it.leverandorid}",
                )
            }
        }
    }

}