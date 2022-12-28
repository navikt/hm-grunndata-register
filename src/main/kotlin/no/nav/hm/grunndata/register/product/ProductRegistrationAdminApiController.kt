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
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

@Secured(Roles.ROLE_ADMIN)
@Controller(ProductRegistrationAdminApiController.API_V1_ADMIN_PRODUCT_REGISTRATIONS)
class ProductRegistrationAdminApiController(private val productRegistrationRepository: ProductRegistrationRepository) {

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
            if (params.contains("adminStatus")) root[ProductRegistration::adminStatus] eq AdminStatus.valueOf(params["adminStatus"]!!)
            if (params.contains("supplierId"))  root[ProductRegistration::supplierId] eq UUID.fromString(params["supplierId"]!!)
            if (params.contains("draft")) root[ProductRegistration::draft] eq DraftStatus.valueOf(params["draft"]!!)
            if (params.contains("createdByUser")) root[ProductRegistration::createdByUser] eq params["createdByUser"]
            if (params.contains("updatedByUser")) root[ProductRegistration::updatedByUser] eq params["updatedByUser"]
        }
    }



    @Get("/{id}")
    suspend fun getProductById(id: UUID): HttpResponse<ProductRegistrationDTO> =
        productRegistrationRepository.findById(id)
            ?.let {
                HttpResponse.ok(it.toDTO()) }
            ?: HttpResponse.notFound()

    @Post("/")
    suspend fun createProduct(@Body registrationDTO: ProductRegistrationDTO, authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
            productRegistrationRepository.findById(registrationDTO.id)?.let {
                HttpResponse.badRequest()
            } ?: run {
                HttpResponse.created(productRegistrationRepository.save(registrationDTO
                    .copy(createdByUser = authentication.name, updatedByUser = authentication.name, createdByAdmin = true)
                    .toEntity()).toDTO())
            }

    @Put("/{id}")
    suspend fun updateProduct(@Body registrationDTO: ProductRegistrationDTO, @PathVariable id: UUID, authentication: Authentication):
            HttpResponse<ProductRegistrationDTO> =
        productRegistrationRepository.findById(id)
                ?.let {
                    val updated = registrationDTO.copy(id = it.id, created = it.created, supplierId = it.supplierId,
                        updatedByUser = authentication.name, updatedBy = REGISTER, createdBy = it.createdBy,
                        createdByAdmin = it.createdByAdmin)
                    HttpResponse.ok(productRegistrationRepository.update(updated.toEntity()).toDTO()) }
                ?: run {
                    HttpResponse.badRequest() }

    @Delete("/{id}")
    suspend fun deleteProduct(@PathVariable id:UUID, authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
        productRegistrationRepository.findById(id)
            ?.let {
                val productDTO = it.productDTO.copy(status = ProductStatus.INACTIVE, expired = LocalDateTime.now().minusMinutes(1L))
                HttpResponse.ok(productRegistrationRepository.update(it.copy(status=RegistrationStatus.DELETED, productDTO = productDTO)).toDTO())}
            ?: HttpResponse.notFound()

}


