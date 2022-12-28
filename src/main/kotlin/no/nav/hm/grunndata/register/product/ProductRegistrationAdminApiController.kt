package no.nav.hm.grunndata.register.product

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType.*
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.user.UserAttribute
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


    @Get("/{?supplier}")
    suspend fun findProducts(@QueryValue supplier: UUID, pageable: Pageable): Page<ProductRegistrationDTO> =
        productRegistrationRepository.findBySupplierId(supplier, pageable).map { it.toDTO() }

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



