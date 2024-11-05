package no.nav.hm.grunndata.register.product

import io.micronaut.data.exceptions.DataAccessException
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.product.batch.ProductExcelImport
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory

@Secured(Roles.ROLE_ADMIN)
@Controller(ProductRegistrationAdminApiControllerV2.API_V2_ADMIN_PRODUCT_REGISTRATIONS)
@Tag(name = "Admin Product V2")
class ProductRegistrationAdminApiControllerV2(
    private val productRegistrationService: ProductRegistrationService,
    private val xlImport: ProductExcelImport,
    private val productDTOMapper: ProductDTOMapper,
) {
    companion object {
        const val API_V2_ADMIN_PRODUCT_REGISTRATIONS = "/admin/api/v2/product/registrations"
        private val LOG = LoggerFactory.getLogger(ProductRegistrationAdminApiControllerV2::class.java)
    }

    @Get("/{id}")
    suspend fun getProductById(id: UUID): HttpResponse<ProductRegistrationDTOV2> =
        productRegistrationService.findById(id)
            ?.let { HttpResponse.ok(productDTOMapper.toDTOV2(it)) } ?: HttpResponse.notFound()

    @Get("/series/{seriesUUID}")
    suspend fun findBySeriesUUIDAndSupplierId(seriesUUID: UUID) =
        productRegistrationService.findAllBySeriesUuid(seriesUUID).sortedBy { it.created }
            .map { productDTOMapper.toDTOV2(it) }

    @Post("/draftWithV3/{seriesUUID}")
    suspend fun createDraft(
        @PathVariable seriesUUID: UUID,
        @Body draftVariant: DraftVariantDTO,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTOV2> =
        try {
            val variant = productRegistrationService.createDraft(seriesUUID, draftVariant, authentication)
            HttpResponse.ok(productDTOMapper.toDTOV2(variant))
        } catch (e: DataAccessException) {
            throw BadRequestException(e.message ?: "Error creating draft")
        } catch (e: Exception) {
            throw BadRequestException("Error creating draft")
        }

    @Put("/{id}")
    suspend fun updateProduct(
        @Body registrationDTO: UpdateProductRegistrationDTO,
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTOV2> =
        try {
            val dto = productDTOMapper.toDTOV2(
                productRegistrationService.updateProduct(
                    registrationDTO,
                    id,
                    authentication
                )
            )
            HttpResponse.ok(dto)
        } catch (dataAccessException: DataAccessException) {
            LOG.error("Got exception while updating product", dataAccessException)
            throw BadRequestException(dataAccessException.message ?: "Got exception while updating product $id")
        } catch (e: Exception) {
            LOG.error("Got exception while updating product", e)
            throw BadRequestException("Got exception while updating product $id")
        }

    @Put("/to-expired/{id}")
    suspend fun setPublishedProductToInactive(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<Any> {
        productRegistrationService.updateRegistrationStatus(id, authentication, RegistrationStatus.INACTIVE)
        return HttpResponse.ok()
    }

    @Put("/to-active/{id}")
    suspend fun setPublishedProductToActive(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<Any> {
        productRegistrationService.updateRegistrationStatus(id, authentication, RegistrationStatus.ACTIVE)
        return HttpResponse.ok()
    }

    @Delete("/draft/delete")
    suspend fun deleteDraftVariants(
        @Body ids: List<UUID>,
        authentication: Authentication,
    ): HttpResponse<Any> {
        productRegistrationService.deleteDraftVariants(ids, authentication)
        return HttpResponse.ok()
    }

    @Post(
        "/excel/import",
        consumes = [MediaType.MULTIPART_FORM_DATA],
        produces = [MediaType.APPLICATION_JSON],
    )
    suspend fun importExcel(
        file: CompletedFileUpload,
        authentication: Authentication,
    ): HttpResponse<List<ProductRegistrationDTOV2>> {
        LOG.info("Importing Excel file ${file.filename} by admin")
        return file.inputStream.use { inputStream ->
            val excelDTOList = xlImport.importExcelFileForRegistration(inputStream)
            LOG.info("found ${excelDTOList.size} products in Excel file")
            val products = productRegistrationService.importExcelRegistrations(excelDTOList, authentication)
            HttpResponse.ok(products.map { productDTOMapper.toDTOV2(it) })
        }
    }
}
