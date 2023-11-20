package no.nav.hm.grunndata.register.product

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.Slice
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.data.runtime.criteria.where
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import no.nav.hm.grunndata.rapid.dto.*
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.supplierId
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

@Secured(Roles.ROLE_ADMIN)
@Controller(ProductRegistrationAdminApiController.API_V1_ADMIN_PRODUCT_REGISTRATIONS)
class ProductRegistrationAdminApiController(private val productRegistrationService: ProductRegistrationService,
                                            private val supplierRegistrationService: SupplierRegistrationService) {

    companion object {
        const val API_V1_ADMIN_PRODUCT_REGISTRATIONS = "/admin/api/v1/product/registrations"
        private val LOG = LoggerFactory.getLogger(ProductRegistrationAdminApiController::class.java)
    }

    @Get("/series/group{?params*}")
    suspend fun findSeriesGroup(@QueryValue params: HashMap<String,String>?,
                                pageable: Pageable): Slice<SeriesGroupDTO> =
        productRegistrationService.findSeriesGroup(pageable)

    @Get("/series/{seriesId}")
    suspend fun findBySeriesIdAndSupplierId(seriesId: String) =
        productRegistrationService.findBySeriesId(seriesId)

    @Get("/{?params*}")
    suspend fun findProducts(@QueryValue params: HashMap<String,String>?,
                             pageable: Pageable): Page<ProductRegistrationDTO> =
        productRegistrationService.findAll(buildCriteriaSpec(params), pageable)

    private fun buildCriteriaSpec(params: HashMap<String, String>?): PredicateSpecification<ProductRegistration>?
            = params?.let {
        where {
            if (params.contains("supplierRef")) root[ProductRegistration::supplierRef] eq params["supplierRef"]
            if (params.contains("hmsArtNr")) root[ProductRegistration::hmsArtNr] eq params["hmsArtNr"]
            if (params.contains("adminStatus")) root[ProductRegistration::adminStatus] eq AdminStatus.valueOf(params["adminStatus"]!!)
            if (params.contains("supplierId"))  root[ProductRegistration::supplierId] eq UUID.fromString(params["supplierId"]!!)
            if (params.contains("draft")) root[ProductRegistration::draftStatus] eq DraftStatus.valueOf(params["draft"]!!)
            if (params.contains("createdByUser")) root[ProductRegistration::createdByUser] eq params["createdByUser"]
            if (params.contains("updatedByUser")) root[ProductRegistration::updatedByUser] eq params["updatedByUser"]
            if (params.contains("title")) criteriaBuilder.like(root[ProductRegistration::title], params["title"])
        }
    }


    @Get("/{id}")
    suspend fun getProductById(id: UUID): HttpResponse<ProductRegistrationDTO> =
        productRegistrationService.findById(id)
            ?.let {
                HttpResponse.ok(it) }
            ?: HttpResponse.notFound()

    @Post("/draft/supplier/{supplierId}{?isAccessory}{?isSparePart}")
    suspend fun draftProduct(supplierId: UUID, authentication: Authentication,
                             @QueryValue(defaultValue = "false") isAccessory: Boolean,
                             @QueryValue(defaultValue = "false") isSparePart: Boolean): HttpResponse<ProductRegistrationDTO> =
        supplierRegistrationService.findById(supplierId)?.let {
            HttpResponse.ok(productRegistrationService.createDraft(supplierId, authentication, isAccessory, isSparePart))
        } ?: throw BadRequestException("$supplierId does not exist")

    @Post("/draftWith/supplier/{supplierId}{?isAccessory}{?isSparePart}")
    suspend fun draftProductWith(supplierId: UUID,
                                 @QueryValue(defaultValue = "false") isAccessory: Boolean,
                                 @QueryValue(defaultValue = "false") isSparePart: Boolean,
                                 @Body draftWith: ProductDraftWithDTO,
                                 authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
        supplierRegistrationService.findById(supplierId)?.let {
            HttpResponse.ok(
                productRegistrationService.createDraftWith(supplierId, authentication, isAccessory, isSparePart, draftWith))
            } ?: throw BadRequestException("$supplierId does not exist")


    @Post("/")
    suspend fun createProduct(@Body registrationDTO: ProductRegistrationDTO, authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
        productRegistrationService.findById(registrationDTO.id)?.let {
                throw BadRequestException("Product registration already exists ${registrationDTO.id}")
            } ?: run {
                val dto = productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(registrationDTO
                    .copy(createdByUser = authentication.name, updatedByUser = authentication.name, createdByAdmin = true,
                        created = LocalDateTime.now(), updated = LocalDateTime.now()), isUpdate = false)
                HttpResponse.created(dto)
            }


    @Put("/{id}")
    suspend fun updateProduct(@Body registrationDTO: ProductRegistrationDTO, @PathVariable id: UUID, authentication: Authentication):
            HttpResponse<ProductRegistrationDTO> =
        productRegistrationService.findById(id)
                ?.let { inDb ->
                    val updated = registrationDTO.copy(adminStatus = inDb.adminStatus,
                        adminInfo = inDb.adminInfo, id = inDb.id, created = inDb.created,
                        updatedByUser = authentication.name, updatedBy = REGISTER, createdBy = inDb.createdBy,
                        createdByAdmin = inDb.createdByAdmin, updated = LocalDateTime.now()
                    )
                    val dto = productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(updated, isUpdate = true)
                    HttpResponse.ok(dto) }
                ?: run {
                    throw BadRequestException("Product registration already exists $id") }

    @Delete("/{id}")
    suspend fun deleteProduct(@PathVariable id:UUID, authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
        productRegistrationService.findById(id)
            ?.let {
                val dto = productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(it.copy(registrationStatus= RegistrationStatus.DELETED,
                    updatedByUser = authentication.name, updatedBy = REGISTER), isUpdate = true)
                HttpResponse.ok(dto)}
            ?: HttpResponse.notFound()


    @Post("/draft/variant/{id}/reference/{supplierRef}")
    suspend fun createProductVariant(@PathVariable id:UUID, supplierRef: String, authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
        productRegistrationService.createProductVariant(id, supplierRef,authentication)?.let {
            HttpResponse.ok(it)
        } ?: HttpResponse.notFound()


    @Put("/approve/{id}")
    suspend fun approveProduct(id:UUID, authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
        productRegistrationService.findById(id)?.let {
            if (it.adminStatus == AdminStatus.APPROVED) throw BadRequestException("$id is already approved")
            if (it.draftStatus != DraftStatus.DONE) throw  BadRequestException("product is not done")
            if (it.registrationStatus != RegistrationStatus.ACTIVE) throw BadRequestException("RegistrationStatus should be Active")
            val dto = productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                it.copy(adminStatus = AdminStatus.APPROVED, adminInfo = AdminInfo(approvedBy = authentication.name, approved = LocalDateTime.now()),
                    updated = LocalDateTime.now(), updatedBy = REGISTER
            ), isUpdate = true)
            HttpResponse.ok(dto)
        }?: HttpResponse.notFound()

}
