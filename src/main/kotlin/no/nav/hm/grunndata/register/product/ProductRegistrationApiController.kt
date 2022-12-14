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

@Secured(Roles.ROLE_SUPPLIER)
@Controller(ProductRegistrationApiController.API_V1_PRODUCT_REGISTRATIONS)
class ProductRegistrationApiController(private val productRegistrationRepository: ProductRegistrationRepository) {

    companion object {
        const val API_V1_PRODUCT_REGISTRATIONS = "/api/v1/product/registrations"
        private val LOG = LoggerFactory.getLogger(ProductRegistrationApiController::class.java)
    }


    @Get("/")
    suspend fun findProducts(authentication: Authentication, pageable: Pageable): Page<ProductRegistrationDTO> =
        productRegistrationRepository.findBySupplierId(userSupplierId(authentication), pageable).map { it.toDTO() }

    @Get("/{id}")
    suspend fun getProductById(id: UUID, authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
        productRegistrationRepository.findByIdAndSupplierId(id, userSupplierId(authentication))
            ?.let {
                HttpResponse.ok(it.toDTO()) }
            ?: HttpResponse.notFound()

    @Post("/")
    suspend fun createProduct(@Body registrationDTO: ProductRegistrationDTO, authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
        if (registrationDTO.supplierId != userSupplierId(authentication) ) HttpResponse.unauthorized()
        else if (registrationDTO.createdByAdmin || registrationDTO.adminStatus == AdminStatus.APPROVED) HttpResponse.unauthorized()
        else
            productRegistrationRepository.findById(registrationDTO.id)?.let {
                HttpResponse.badRequest()
            } ?: run {
                HttpResponse.created(productRegistrationRepository.save(registrationDTO
                    .copy(updatedByUser =  authentication.name, createdByUser = authentication.name )
                    .toEntity()).toDTO())
            }

    @Put("/{id}")
    suspend fun updateProduct(@Body registrationDTO: ProductRegistrationDTO, @PathVariable id: UUID, authentication: Authentication):
            HttpResponse<ProductRegistrationDTO> =
        if (registrationDTO.supplierId != userSupplierId(authentication) ) HttpResponse.unauthorized()
        else productRegistrationRepository.findByIdAndSupplierId(id,userSupplierId(authentication))
                ?.let {
                    HttpResponse.ok(productRegistrationRepository.update(registrationDTO
                        .copy(id = it.id, created = it.created, supplierId = it.supplierId,
                        updatedBy = REGISTER, updatedByUser = authentication.name, createdByUser = it.createdByUser,
                        createdBy = it.createdBy, createdByAdmin = it.createdByAdmin, adminStatus = it.adminStatus,
                        adminInfo = it.adminInfo)
                        .toEntity()).toDTO()) }
                ?: run {
                    HttpResponse.badRequest() }

    @Delete("/{id}")
    suspend fun deleteProduct(@PathVariable id:UUID, authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
        productRegistrationRepository.findByIdAndSupplierId(id, userSupplierId(authentication))
            ?.let {
                val productDTO = it.productDTO.copy(status = ProductStatus.INACTIVE, expired = LocalDateTime.now().minusMinutes(1L))
                HttpResponse.ok(productRegistrationRepository.update(it
                    .copy(status=RegistrationStatus.DELETED, updatedByUser = authentication.name, productDTO = productDTO))
                    .toDTO()) }
            ?: HttpResponse.notFound()


    private fun userSupplierId(authentication: Authentication) = UUID.fromString(
        authentication.attributes[UserAttribute.SUPPLIER_ID] as String )

}




