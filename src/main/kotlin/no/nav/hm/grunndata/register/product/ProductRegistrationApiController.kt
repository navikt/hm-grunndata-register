package no.nav.hm.grunndata.register.product

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.user.UserAttribute
import org.slf4j.LoggerFactory
import java.util.*

@Secured(Roles.ROLE_SUPPLIER)
@Controller("/api/v1/product")
class ProductRegistrationApi(private val productRegistrationRepository: ProductRegistrationRepository) {

    private val LOG = LoggerFactory.getLogger(ProductRegistrationApi::class.java)

    @Get("/{id}")
    suspend fun getProductById(id: UUID, authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
        productRegistrationRepository.findByIdAndSupplierId(id, userSupplierId(authentication))
            ?.let {
                HttpResponse.ok(it.toDTO()) }
            ?: HttpResponse.notFound()

    @Post("/")
    suspend fun createProduct(@Body registrationDTO: ProductRegistrationDTO, authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
        if (registrationDTO.supplierId != userSupplierId(authentication) ) HttpResponse.unauthorized()
        else
            productRegistrationRepository.findById(registrationDTO.id)?.let {
                HttpResponse.badRequest()
            } ?: run {
                HttpResponse.created(productRegistrationRepository.save(registrationDTO.toEntity()).toDTO())
            }

    @Put("/{id}")
    suspend fun updateProduct(@Body registrationDTO: ProductRegistrationDTO, @PathVariable id: UUID, authentication: Authentication):
            HttpResponse<ProductRegistrationDTO> =
        if (registrationDTO.supplierId != userSupplierId(authentication) ) HttpResponse.unauthorized()
        else productRegistrationRepository.findById(id)
                ?.let {
                    val updated = registrationDTO.copy(id = it.id, created = it.created)
                    HttpResponse.ok(productRegistrationRepository.update(updated.toEntity()).toDTO()) }
                ?: run {
                    HttpResponse.badRequest() }

    @Delete("/{id}")
    suspend fun deleteProduct(id:UUID, authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
        productRegistrationRepository.findByIdAndSupplierId(id, userSupplierId(authentication))
            ?.let {
                HttpResponse.ok(productRegistrationRepository.update(it.copy(status=RegistrationStatus.DELETED)).toDTO())}
            ?: HttpResponse.notFound()

}

private fun ProductRegistrationDTO.toEntity(): ProductRegistration = ProductRegistration(id = id,
    supplierId = supplierId, supplierRef =supplierRef, HMSArtNr = HMSArtNr, title = title, draft = draft,
    adminStatus = adminStatus, message = message, adminInfo = adminInfo, created = created, updated = updated,
    published = published, expired = expired, createdBy = createdBy, updatedBy = updatedBy,
    createdByAdmin = createdByAdmin, productDTO = productDTO)

private fun ProductRegistration.toDTO(): ProductRegistrationDTO = ProductRegistrationDTO(
    id = id, supplierId= supplierId, supplierRef =supplierRef, HMSArtNr = HMSArtNr, title = title, draft = draft,
    adminStatus = adminStatus, message = message, adminInfo = adminInfo, created = created, updated = updated,
    published = published, expired = expired, createdBy = createdBy, updatedBy = updatedBy,
    createdByAdmin = createdByAdmin, productDTO = productDTO
)


fun userSupplierId(authentication: Authentication) =
    authentication.attributes[UserAttribute.SUPPLIER_ID] as UUID