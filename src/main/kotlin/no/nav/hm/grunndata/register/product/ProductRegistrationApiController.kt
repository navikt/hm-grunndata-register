package no.nav.hm.grunndata.register.product

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.Slice
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.data.runtime.criteria.where
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.product.batch.ProductExcelExport
import no.nav.hm.grunndata.register.product.batch.ProductExcelImport
import no.nav.hm.grunndata.register.product.batch.toRegistrationDTO
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.supplierId
import no.nav.hm.grunndata.register.series.SeriesGroupDTO
import org.apache.commons.io.output.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

@Secured(Roles.ROLE_SUPPLIER)
@Controller(ProductRegistrationApiController.API_V1_PRODUCT_REGISTRATIONS)
class ProductRegistrationApiController(private val productRegistrationService: ProductRegistrationService,
                                       private val xlExport: ProductExcelExport,
                                       private val xlImport: ProductExcelImport) {

    companion object {
        const val API_V1_PRODUCT_REGISTRATIONS = "/vendor/api/v1/product/registrations"
        private val LOG = LoggerFactory.getLogger(ProductRegistrationApiController::class.java)
    }

    @Get("/series/group{?params*}")
    suspend fun findSeriesGroup(@QueryValue params: HashMap<String,String>?,
                                pageable: Pageable, authentication: Authentication): Slice<SeriesGroupDTO> =
        productRegistrationService.findSeriesGroup(authentication.supplierId(), pageable)


    @Get("/series/{seriesId}")
    suspend fun findBySeriesIdAndSupplierId(seriesId: String, authentication: Authentication) =
        productRegistrationService.findBySeriesIdAndSupplierId(seriesId, authentication.supplierId()).sortedBy { it.created }

    @Get("/{?params*}")
    suspend fun findProducts(@QueryValue params: HashMap<String,String>?,
                             pageable: Pageable, authentication: Authentication): Page<ProductRegistrationDTO> =
        productRegistrationService.findAll(buildCriteriaSpec(params, authentication.supplierId()), pageable)


    private fun buildCriteriaSpec(params: HashMap<String, String>?, supplierId: UUID): PredicateSpecification<ProductRegistration>?
            = params?.let {
        where {
            root[ProductRegistration::supplierId] eq supplierId
            if (params.contains("supplierRef")) root[ProductRegistration::supplierRef] eq params["supplierRef"]
            if (params.contains("hmsArtNr")) root[ProductRegistration::hmsArtNr] eq params["hmsArtNr"]
            if (params.contains("draft")) root[ProductRegistration::draftStatus] eq DraftStatus.valueOf(params["draft"]!!)
        }.and { root, criteriaBuilder ->
            if (params.contains("title")) criteriaBuilder.like(root[ProductRegistration::title], params["title"]) else null }
    }

    @Get("/{id}")
    suspend fun getProductById(id: UUID, authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
        productRegistrationService.findByIdAndSupplierId(id, authentication.supplierId())
            ?.let {
                HttpResponse.ok(it) }
            ?: HttpResponse.notFound()

    @Post("/")
    suspend fun createProduct(@Body registrationDTO: ProductRegistrationDTO, authentication: Authentication):
            HttpResponse<ProductRegistrationDTO> =
        if (registrationDTO.supplierId != authentication.supplierId()) {
            LOG.warn("Got unauthorized attempt for ${registrationDTO.supplierId}")
            HttpResponse.unauthorized()
        }
        else if (registrationDTO.createdByAdmin || registrationDTO.adminStatus == AdminStatus.APPROVED) HttpResponse.unauthorized()
        else
            productRegistrationService.findById(registrationDTO.id)?.let {
                throw BadRequestException("Product registration already exists ${registrationDTO.id}")
            } ?: run {
                val dto = productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(registrationDTO
                    .copy(updatedByUser =  authentication.name, createdByUser = authentication.name,
                        created = LocalDateTime.now(), updated = LocalDateTime.now()), isUpdate = false)
                HttpResponse.created(dto)
            }

    @Put("/{id}")
    suspend fun updateProduct(@Body registrationDTO: ProductRegistrationDTO, @PathVariable id: UUID, authentication: Authentication):
            HttpResponse<ProductRegistrationDTO> =
        if (registrationDTO.supplierId != authentication.supplierId()) HttpResponse.unauthorized()
        else if (registrationDTO.id != id) throw BadRequestException("Product id $id does not match ${registrationDTO.id}")
        else productRegistrationService.findByIdAndSupplierId(id, registrationDTO.supplierId)
                ?.let { inDb ->
                    val dto = productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(registrationDTO
                        .copy(id = inDb.id, created = inDb.created,
                            updatedBy = REGISTER, updatedByUser = authentication.name, createdByUser = inDb.createdByUser,
                            createdBy = inDb.createdBy, createdByAdmin = inDb.createdByAdmin, adminStatus = inDb.adminStatus,
                            adminInfo = inDb.adminInfo, updated = LocalDateTime.now()), isUpdate = true)
                    HttpResponse.ok(dto) }
                ?: run {
                    throw BadRequestException("Product does not exists $id") }

    @Delete("/{id}")
    suspend fun deleteProduct(@PathVariable id:UUID, authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
        productRegistrationService.findByIdAndSupplierId(id, authentication.supplierId())
            ?.let {
                val deleteDTO = productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(it
                    .copy(registrationStatus = RegistrationStatus.DELETED, updatedByUser = authentication.name), isUpdate = true)
                HttpResponse.ok(deleteDTO)
            } ?: HttpResponse.notFound()

    @Post("/draft{?isAccessory}{?isSparePart}")
    suspend fun draftProduct(@QueryValue(defaultValue = "false") isAccessory: Boolean,
                             @QueryValue(defaultValue = "false") isSparePart: Boolean, authentication: Authentication): HttpResponse<ProductRegistrationDTO> {
        val supplierId = authentication.supplierId()
        return HttpResponse.ok(productRegistrationService.createDraft(supplierId, authentication, isAccessory, isSparePart))
    }

    @Post("/draftWith{?isAccessory}{?isSparePart}")
    suspend fun draftProductWith(@QueryValue(defaultValue = "false") isAccessory: Boolean,
                                 @QueryValue(defaultValue = "false") isSparePart: Boolean,
                                 @Body draftWith: ProductDraftWithDTO,
                                 authentication: Authentication): HttpResponse<ProductRegistrationDTO> {
        val supplierId = authentication.supplierId()
        return HttpResponse.ok(
            productRegistrationService.createDraftWith(supplierId, authentication, isAccessory, isSparePart, draftWith)
        )
    }

    @Post("/draft/variant/{id}")
    suspend fun createProductVariant(@PathVariable id:UUID, @Body draftVariant: DraftVariantDTO, authentication: Authentication): HttpResponse<ProductRegistrationDTO> {
        return try {
            productRegistrationService.createProductVariant(id, draftVariant, authentication)?.let {
                HttpResponse.ok(it)
            } ?: HttpResponse.notFound()
        } catch (e: Exception) {
            LOG.error("Got exception while creating variant ${draftVariant.supplierRef}", e)
            throw BadRequestException("Could not create variant for ${draftVariant.supplierRef}, already exists")
        }
    }

    @Post("/excel/export")
    suspend fun createExport(@Body uuids: List<UUID>, authentication: Authentication): HttpResponse<ByteArrayOutputStream> {
        val products = uuids.map { productRegistrationService.findById(it)}.filterNotNull()
        products.forEach {
            if (it.supplierId != authentication.supplierId()) {
                throw BadRequestException("Unauthorized access to product ${it.id}")
            }
        }
        return ByteArrayOutputStream().use {
            xlExport.createWorkbookToOutputStream(products, it)
            HttpResponse.ok(it)
        }
    }

    @Post("/excel/import")
    suspend fun importExcel(file: CompletedFileUpload,
                            @QueryValue dryRun: Boolean = true,
                            authentication: Authentication): HttpResponse<List<ProductRegistrationDTO>> {
        LOG.info("Importing Excel file ${file.filename} for supplierId ${authentication.supplierId()}")
        return file.inputStream.use {inputStream ->
            val excelDTOList = xlImport.importExcelFileForRegistration(inputStream)
            HttpResponse.ok(excelDTOList.map { it.toRegistrationDTO() })
        }
    }
}

data class ProductDraftWithDTO(val title: String, val text: String, val isoCategory: String)
data class DraftVariantDTO(val articleName: String, val supplierRef: String)