package no.nav.hm.grunndata.register.api

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.rules.SecurityRule
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationDTO
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import no.nav.hm.grunndata.register.product.RegistrationStatus
import org.slf4j.LoggerFactory
import java.util.*

@Secured(SecurityRule.IS_AUTHENTICATED)
@Controller("/api/v1/product")
class ProductRegistrationApi(private val productRegistrationRepository: ProductRegistrationRepository,
                             private val authentication: Authentication) {

    private val LOG = LoggerFactory.getLogger(ProductRegistrationApi::class.java)

    @Get("/{id}")
    suspend fun getProductById(id: UUID): HttpResponse<ProductRegistrationDTO> =
        productRegistrationRepository.findById(id)?.let {
            HttpResponse.ok(it.toDTO())
        } ?: HttpResponse.notFound()

    @Post("/")
    suspend fun createProduct(@Body registrationDTO: ProductRegistrationDTO): HttpResponse<ProductRegistrationDTO> =
        productRegistrationRepository.findById(registrationDTO.id)?.let {
            HttpResponse.badRequest()
        } ?: run {
            HttpResponse.created(productRegistrationRepository.save(registrationDTO.toEntity()).toDTO())
        }

    @Put("/{id}")
    suspend fun updateProduct(@Body registrationDTO: ProductRegistrationDTO, @PathVariable id: UUID):
            HttpResponse<ProductRegistrationDTO> =
        productRegistrationRepository.findById(id)?.let {
            val updated = registrationDTO.copy(id = it.id, created = it.created)
            HttpResponse.ok(productRegistrationRepository.update(updated.toEntity()).toDTO())
        } ?: run {
            HttpResponse.badRequest()
        }

    @Delete("/{id}")
    suspend fun deleteProduct(id:UUID): HttpResponse<String> =
        productRegistrationRepository.findById(id)?.let {
            productRegistrationRepository.update(it.copy(status=RegistrationStatus.DELETED))
            HttpResponse.ok()
        } ?: HttpResponse.notFound()

}

private fun ProductRegistrationDTO.toEntity(): ProductRegistration = ProductRegistration(id = id,
    supplierUuid = supplierUuid, supplierRef =supplierRef, HMSArtNr = HMSArtNr, title = title, draft = draft,
    adminStatus = adminStatus, message = message, adminInfo = adminInfo, created = created, updated = updated,
    published = published, expired = expired, createdBy = createdBy, updatedBy = updatedBy,
    createdByAdmin = createdByAdmin, productDTO = productDTO)

private fun ProductRegistration.toDTO(): ProductRegistrationDTO = ProductRegistrationDTO(
    id = id, supplierUuid= supplierUuid, supplierRef =supplierRef, HMSArtNr = HMSArtNr, title = title, draft = draft,
    adminStatus = adminStatus, message = message, adminInfo = adminInfo, created = created, updated = updated,
    published = published, expired = expired, createdBy = createdBy, updatedBy = updatedBy,
    createdByAdmin = createdByAdmin, productDTO = productDTO
)
