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
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.api.BadRequestException
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

@Secured(Roles.ROLE_ADMIN)
@Controller(ProductRegistrationAdminApiController.API_V1_ADMIN_PRODUCT_REGISTRATIONS)
class ProductRegistrationAdminApiController(private val productRegistrationService: ProductRegistrationService,
                                            private val supplierService: SupplierService) {

    companion object {
        const val API_V1_ADMIN_PRODUCT_REGISTRATIONS = "/api/v1/admin/product/registrations"
        private val LOG = LoggerFactory.getLogger(ProductRegistrationAdminApiController::class.java)
    }

    @Get("/{?params*}")
    suspend fun findProducts(@QueryValue params: HashMap<String,String>?,
                             pageable: Pageable): Page<ProductRegistrationDTO> =
        productRegistrationService.findAll(params, pageable)



    @Get("/{id}")
    suspend fun getProductById(id: UUID): HttpResponse<ProductRegistrationDTO> =
        productRegistrationService.findById(id)
            ?.let {
                HttpResponse.ok(it) }
            ?: HttpResponse.notFound()

    @Post("/draft/supplier/{supplierId}/reference/{supplierRef}{?isAccessory}{?isSparePart}")
    suspend fun draftProduct(supplierId: UUID, supplierRef: String, authentication: Authentication,
                             @QueryValue(defaultValue = "false") isAccessory: Boolean,
                             @QueryValue(defaultValue = "false") isSparePart: Boolean): HttpResponse<ProductRegistrationDTO> =
        supplierService.findById(supplierId)?.let {
            if (productRegistrationService.findBySupplierIdAndSupplierRef(supplierId, supplierRef)!=null) {
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
            HttpResponse.ok(productRegistrationService.save(registration))
        } ?: throw BadRequestException("$supplierId does not exist")


    @Post("/")
    suspend fun createProduct(@Body registrationDTO: ProductRegistrationDTO, authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
        productRegistrationService.findById(registrationDTO.id)?.let {
                throw BadRequestException("Product registration already exists ${registrationDTO.id}")
            } ?: run {
                val dto = productRegistrationService.saveAndPushToKafka(registrationDTO
                    .copy(createdByUser = authentication.name, updatedByUser = authentication.name, createdByAdmin = true,
                        created = LocalDateTime.now(), updated = LocalDateTime.now()), isUpdate = false)
                HttpResponse.created(dto)
            }




    @Put("/{id}")
    suspend fun updateProduct(@Body registrationDTO: ProductRegistrationDTO, @PathVariable id: UUID, authentication: Authentication):
            HttpResponse<ProductRegistrationDTO> =
        productRegistrationService.findById(id)
                ?.let { inDb ->
                    val updated = registrationDTO.copy(
                        id = inDb.id, created = inDb.created,
                        updatedByUser = authentication.name, updatedBy = REGISTER, createdBy = inDb.createdBy,
                        createdByAdmin = inDb.createdByAdmin, updated = LocalDateTime.now()
                    )
                    val dto = productRegistrationService.saveAndPushToKafka(updated, isUpdate = true)
                    HttpResponse.ok(dto) }
                ?: run {
                    throw BadRequestException("Product registration already exists $id") }

    @Delete("/{id}")
    suspend fun deleteProduct(@PathVariable id:UUID, authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
        productRegistrationService.findById(id)
            ?.let {
                val dto = productRegistrationService.saveAndPushToKafka(it.copy(registrationStatus= RegistrationStatus.DELETED), isUpdate = true)
                HttpResponse.ok(dto)}
            ?: HttpResponse.notFound()


    @Post("/draft/variant/{id}/reference/{supplierRef}")
    suspend fun createProductVariant(@PathVariable id:UUID, supplierRef: String, authentication: Authentication): HttpResponse<ProductRegistrationDTO> {
        return productRegistrationService.createProductVariant(id, supplierRef,authentication)?.let {
            HttpResponse.ok(it)
        } ?: HttpResponse.notFound()
    }


}
