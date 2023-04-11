package no.nav.hm.grunndata.register.product

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.data.runtime.criteria.where
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType.*
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import no.nav.hm.grunndata.rapid.dto.*
import no.nav.hm.grunndata.register.api.BadRequestException
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

@Secured(Roles.ROLE_ADMIN)
@Controller(ProductRegistrationAdminApiController.API_V1_ADMIN_PRODUCT_REGISTRATIONS)
class ProductRegistrationAdminApiController(private val productRegistrationRepository: ProductRegistrationRepository,
                                            private val supplierService: SupplierService,
                                            private val productRegistrationHandler: ProductRegistrationHandler) {

    companion object {
        const val API_V1_ADMIN_PRODUCT_REGISTRATIONS = "/api/v1/admin/product/registrations"
        private val LOG = LoggerFactory.getLogger(ProductRegistrationAdminApiController::class.java)
    }

    @Get("/{?params*}")
    suspend fun findProducts(@QueryValue params: HashMap<String,String>?,
                             pageable: Pageable): Page<ProductRegistrationDTO> =
        productRegistrationRepository.findAll(buildCriteriaSpec(params), pageable).map { it.toDTO() }


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
        productRegistrationRepository.findById(id)
            ?.let {
                HttpResponse.ok(it.toDTO()) }
            ?: HttpResponse.notFound()

    @Post("/draft/supplier/{supplierId}/reference/{supplierRef}{?isAccessory}{?isSparePart}")
    suspend fun draftProduct(supplierId: UUID, supplierRef: String, authentication: Authentication,
                             @QueryValue(defaultValue = "false") isAccessory: Boolean,
                             @QueryValue(defaultValue = "false") isSparePart: Boolean): HttpResponse<ProductRegistrationDTO> =
        supplierService.findById(supplierId)?.let {
            if (productRegistrationRepository.findBySupplierIdAndSupplierRef(supplierId, supplierRef)!=null) {
                throw BadRequestException("$supplierId and $supplierRef duplicate error")
            }
            val productId = UUID.randomUUID()
            val product = ProductData (
                    accessory = isAccessory,
                    sparePart = isSparePart,
                    seriesId = productId.toString(),
                    isoCategory = "", attributes = Attributes (
                    shortdescription = "kort beskrivelse",
                    text = "en lang beskrivelse",
                    compatible = if (isSparePart || isAccessory) listOf(CompatibleAttribute(hmsArtNr = "", supplierRef = "")) else null
                ))
            val registration = ProductRegistrationDTO(
                id = productId,
                supplierId = supplierId,
                supplierRef = supplierRef,
                hmsArtNr = "",
                title = "",
                articleName = "",
                createdBy = REGISTER,
                updatedBy = REGISTER,
                message = null,
                published = LocalDateTime.now(),
                expired = LocalDateTime.now().plusYears(10),
                productData = product,
                createdByUser = authentication.name,
                updatedByUser = authentication.name,
                createdByAdmin = true,
                version = 0)
            HttpResponse.ok(productRegistrationRepository.save(registration.toEntity()).toDTO())
        } ?: throw BadRequestException("$supplierId does not exist")


    @Post("/")
    suspend fun createProduct(@Body registrationDTO: ProductRegistrationDTO, authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
            productRegistrationRepository.findById(registrationDTO.id)?.let {
                throw BadRequestException("Product registration already exists ${registrationDTO.id}")
            } ?: run {
                val dto = productRegistrationRepository.save(registrationDTO
                    .copy(createdByUser = authentication.name, updatedByUser = authentication.name, createdByAdmin = true,
                        created = LocalDateTime.now(), updated = LocalDateTime.now()).toEntity()).toDTO()
                productRegistrationHandler.pushToRapidIfNotDraft(dto)
                HttpResponse.created(dto)
            }




    @Put("/{id}")
    suspend fun updateProduct(@Body registrationDTO: ProductRegistrationDTO, @PathVariable id: UUID, authentication: Authentication):
            HttpResponse<ProductRegistrationDTO> =
        productRegistrationRepository.findById(id)
                ?.let { inDb ->
                    val updated = registrationDTO.copy(
                        id = inDb.id, created = inDb.created,
                        updatedByUser = authentication.name, updatedBy = REGISTER, createdBy = inDb.createdBy,
                        createdByAdmin = inDb.createdByAdmin, updated = LocalDateTime.now()
                    )
                    val dto = productRegistrationRepository.update(updated.toEntity()).toDTO()
                    productRegistrationHandler.pushToRapidIfNotDraft(dto)
                    HttpResponse.ok(dto) }
                ?: run {
                    throw BadRequestException("Product registration already exists $id") }

    @Delete("/{id}")
    suspend fun deleteProduct(@PathVariable id:UUID, authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
        productRegistrationRepository.findById(id)
            ?.let {
                val dto = productRegistrationRepository.update(it.copy(registrationStatus= RegistrationStatus.DELETED)).toDTO()
                productRegistrationHandler.pushToRapidIfNotDraft(dto)
                HttpResponse.ok(dto)}
            ?: HttpResponse.notFound()

    @Get("/template/{id}")
    suspend fun useProductTemplate(@PathVariable id: UUID, authentication: Authentication): HttpResponse<ProductRegistrationDTO> {
        return productRegistrationRepository.findById(id)?.let {
            HttpResponse.ok(productRegistrationHandler.makeTemplateOf(it, authentication))
        } ?: HttpResponse.notFound()
    }

}
