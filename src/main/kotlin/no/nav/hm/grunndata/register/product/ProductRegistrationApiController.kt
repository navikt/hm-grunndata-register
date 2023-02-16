package no.nav.hm.grunndata.register.product

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType.*
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.ProductRegistrationDTO
import no.nav.hm.grunndata.rapid.dto.ProductStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.user.UserAttribute
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

@Secured(Roles.ROLE_SUPPLIER)
@Controller(ProductRegistrationApiController.API_V1_PRODUCT_REGISTRATIONS)
class ProductRegistrationApiController(private val productRegistrationRepository: ProductRegistrationRepository,
                                       private val kafkaRapidHandler: ProductRegistrationRapidHandler) {

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
                val dto = productRegistrationRepository.save(registrationDTO
                    .copy(updatedByUser =  authentication.name, createdByUser = authentication.name )
                    .toEntity()).toDTO()
                kafkaRapidHandler.pushProductToKafka(dto, EventName.productRegistration)
                HttpResponse.created(dto)
            }

    @Put("/{id}")
    suspend fun updateProduct(@Body registrationDTO: ProductRegistrationDTO, @PathVariable id: UUID, authentication: Authentication):
            HttpResponse<ProductRegistrationDTO> =
        if (registrationDTO.supplierId != userSupplierId(authentication) ) HttpResponse.unauthorized()
        else productRegistrationRepository.findByIdAndSupplierId(id,userSupplierId(authentication))
                ?.let {
                    val dto = productRegistrationRepository.update(registrationDTO
                        .copy(id = it.id, created = it.created, supplierId = it.supplierId,
                            updatedBy = REGISTER, updatedByUser = authentication.name, createdByUser = it.createdByUser,
                            createdBy = it.createdBy, createdByAdmin = it.createdByAdmin, adminStatus = it.adminStatus,
                            adminInfo = it.adminInfo)
                        .toEntity()).toDTO()
                    kafkaRapidHandler.pushProductToKafka(dto, EventName.productRegistration)
                    HttpResponse.ok(dto) }
                ?: run {
                    HttpResponse.badRequest() }

    @Delete("/{id}")
    suspend fun deleteProduct(@PathVariable id:UUID, authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
        productRegistrationRepository.findByIdAndSupplierId(id, userSupplierId(authentication))
            ?.let {
                val productDTO = it.productDTO.copy(status = ProductStatus.INACTIVE,
                    expired = LocalDateTime.now().minusMinutes(1L), updatedBy = REGISTER, updated = LocalDateTime.now())
                val deleteDTO = productRegistrationRepository.update(it
                    .copy(status= RegistrationStatus.DELETED, updatedByUser = authentication.name, productDTO = productDTO))
                    .toDTO()
                HttpResponse.ok(deleteDTO)
            }
            ?: HttpResponse.notFound()


    private fun userSupplierId(authentication: Authentication) = UUID.fromString(
        authentication.attributes[UserAttribute.SUPPLIER_ID] as String )

}




