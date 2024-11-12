package no.nav.hm.grunndata.register.product.batch

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.http.server.types.files.StreamedFile
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.product.ProductDTOMapper
import no.nav.hm.grunndata.register.product.ProductRegistrationDTO
import no.nav.hm.grunndata.register.product.ProductRegistrationDryRunDTO
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.product.batch.ProductExcelAdminApi.Companion.API_V1_ADMIN_PRODUCT_REGISTRATIONS_EXCEL
import no.nav.hm.grunndata.register.security.Roles
import org.apache.commons.io.output.ByteArrayOutputStream
import org.slf4j.LoggerFactory

@Secured(Roles.ROLE_ADMIN)
@Controller(API_V1_ADMIN_PRODUCT_REGISTRATIONS_EXCEL)
@Tag(name = "Product Excel API")
class ProductExcelAdminApi(
    private val productRegistrationService: ProductRegistrationService,
    private val xlImport: ProductExcelImport,
    private val xlExport2: ProductExcelExport2,
    private val productDTOMapper: ProductDTOMapper,
    private val excelExportMapper: ExcelExportMapper,
) {
    companion object {
        const val API_V1_ADMIN_PRODUCT_REGISTRATIONS_EXCEL = "/admin/api/v1/product/registrations/excel"
        private val LOG = LoggerFactory.getLogger(ProductExcelAdminApi::class.java)
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
        if (products.isEmpty()) throw BadRequestException("No products found")

        val excelExportDtos = excelExportMapper.mapToExportDtos(products.map { productDTOMapper.toDTO(it) })

        val id = UUID.randomUUID()
        LOG.info("Generating Excel file: $id.xlsx")
        return ByteArrayOutputStream().use { byteStream ->
            xlExport2.createWorkbookToOutputStream(excelExportDtos, byteStream)
            HttpResponse.ok(StreamedFile(byteStream.toInputStream(), MediaType.MICROSOFT_EXCEL_OPEN_XML_TYPE))
                .header("Content-Disposition", "attachment; filename=$id.xlsx")
        }
    }

    @Post(
        "/import",
        consumes = [MediaType.MULTIPART_FORM_DATA],
        produces = [MediaType.APPLICATION_JSON],
    )
    suspend fun importExcel(
        file: CompletedFileUpload,
        authentication: Authentication,
    ): HttpResponse<List<ProductRegistrationDTO>> {
        LOG.info("Importing Excel file ${file.filename} by admin")
        return file.inputStream.use { inputStream ->
            val excelDTOList = xlImport.importExcelFileForRegistration(inputStream)
            LOG.info("found ${excelDTOList.size} products in Excel file")
            val products = productRegistrationService.importExcelRegistrations(excelDTOList, authentication)
            HttpResponse.ok(products.map { productDTOMapper.toDTO(it) })
        }
    }

    @Post(
        "/import-dryrun",
        consumes = [MediaType.MULTIPART_FORM_DATA],
        produces = [MediaType.APPLICATION_JSON],
    )
    suspend fun importExcelDryrun(
        file: CompletedFileUpload,
        authentication: Authentication,
    ): HttpResponse<List<ProductRegistrationDryRunDTO>> {
        LOG.info("Dryrun - Importing Excel file ${file.filename} by admin")
        return file.inputStream.use { inputStream ->
            val excelDTOList = xlImport.importExcelFileForRegistration(inputStream)
            LOG.info("found ${excelDTOList.size} products in Excel file")
            val products = productRegistrationService.importDryRunExcelRegistrations(excelDTOList, authentication)
            HttpResponse.ok(products)
        }
    }

}
