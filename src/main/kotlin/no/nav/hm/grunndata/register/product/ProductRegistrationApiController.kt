package no.nav.hm.grunndata.register.product

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType.*
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import no.nav.hm.grunndata.rapid.dto.*
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.RegisterRapidPushService
import no.nav.hm.grunndata.register.api.BadRequestException
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierRepository
import no.nav.hm.grunndata.register.supplier.toDTO
import no.nav.hm.grunndata.register.user.UserAttribute
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

@Secured(Roles.ROLE_SUPPLIER)
@Controller(ProductRegistrationApiController.API_V1_PRODUCT_REGISTRATIONS)
class ProductRegistrationApiController(private val productRegistrationRepository: ProductRegistrationRepository,
                                       private val registerRapidPushService: RegisterRapidPushService,
                                       private val supplierRepository: SupplierRepository,
                                       private val productRegistrationHandler: ProductRegistrationHandler) {

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
        if (registrationDTO.supplierId != userSupplierId(authentication) ||
            registrationDTO.productDTO.supplier.id != userSupplierId(authentication)) HttpResponse.unauthorized()
        else if (registrationDTO.createdByAdmin || registrationDTO.adminStatus == AdminStatus.APPROVED) HttpResponse.unauthorized()
        else
            productRegistrationRepository.findById(registrationDTO.id)?.let {
                throw BadRequestException("Product registration already exists ${registrationDTO.id}")
            } ?: run {
                val dto = productRegistrationRepository.save(registrationDTO
                    .copy(updatedByUser =  authentication.name, createdByUser = authentication.name,
                        created = LocalDateTime.now(), updated = LocalDateTime.now())
                    .toEntity()).toDTO()
                pushToRapidIfNotDraft(dto)
                HttpResponse.created(dto)
            }
    private fun pushToRapidIfNotDraft(dto: ProductRegistrationDTO) {
        if (dto.draftStatus == DraftStatus.DONE) {
            registerRapidPushService.pushDTOToKafka(dto, EventName.registeredProductV1)
        }
    }
    @Put("/{id}")
    suspend fun updateProduct(@Body registrationDTO: ProductRegistrationDTO, @PathVariable id: UUID, authentication: Authentication):
            HttpResponse<ProductRegistrationDTO> =
        if (registrationDTO.supplierId != userSupplierId(authentication) ||
            registrationDTO.productDTO.supplier.id != userSupplierId(authentication)) HttpResponse.unauthorized()
        else productRegistrationRepository.findByIdAndSupplierId(id,userSupplierId(authentication))
                ?.let {
                    val dto = productRegistrationRepository.update(registrationDTO
                        .copy(id = it.id, created = it.created, supplierId = it.supplierId,
                            updatedBy = REGISTER, updatedByUser = authentication.name, createdByUser = it.createdByUser,
                            createdBy = it.createdBy, createdByAdmin = it.createdByAdmin, adminStatus = it.adminStatus,
                            adminInfo = it.adminInfo, updated = LocalDateTime.now(),
                            productDTO = it.productDTO.copy(updated = LocalDateTime.now(),
                                status = if (it.adminStatus == AdminStatus.PENDING) ProductStatus.INACTIVE
                                    else registrationDTO.productDTO.status
                            ))
                        .toEntity()).toDTO()
                    pushToRapidIfNotDraft(dto)
                    HttpResponse.ok(dto) }
                ?: run {
                    throw BadRequestException("Product does not exists $id") }

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

    @Get("/draft/{supplierRef}")
    suspend fun draftProduct(@PathVariable supplierRef: String, authentication: Authentication): HttpResponse<ProductRegistrationDTO> {
        val supplierId = userSupplierId(authentication)
        productRegistrationRepository.findBySupplierIdAndSupplierRef(supplierId, supplierRef)?.let {
            throw BadRequestException("$supplierId and $supplierRef already exists")
        } ?: run {
            val supplier = supplierRepository.findById(supplierId)!!.toDTO()
            val productId = UUID.randomUUID()
            val product = ProductDTO(id = productId, updatedBy = REGISTER, createdBy = REGISTER, title = "", articleName= "",  status = ProductStatus.INACTIVE,
                supplier = supplier, supplierRef = supplierRef, identifier = productId.toString(),
                seriesId = productId.toString(), isoCategory = "", attributes = mapOf(
                    AttributeNames.shortdescription to "kort beskrivelse", AttributeNames.text to "en lang beskrivelse")
            )
            val registration = ProductRegistrationDTO(id = productId, supplierId= supplier.id, hmsArtNr = null,   createdBy = REGISTER,
                updatedBy = REGISTER, supplierRef = supplierRef, message = null, title = product.title,  articleName = product.articleName, published = product.published,
                expired = product.expired, productDTO = product)
            return HttpResponse.ok(registration)
        }
    }

    @Get("/template/{id}")
    suspend fun useProductTemplate(@PathVariable id: UUID, authentication: Authentication): HttpResponse<ProductRegistrationDTO> {
        val supplierId = userSupplierId(authentication)
        return productRegistrationRepository.findByIdAndSupplierId(id, supplierId)?.let {
            HttpResponse.ok(productRegistrationHandler.makeTemplateOf(it, authentication))
        } ?: HttpResponse.notFound()
    }

    private fun userSupplierId(authentication: Authentication) = UUID.fromString(
        authentication.attributes[UserAttribute.SUPPLIER_ID] as String )

}

fun Authentication.isAdmin(): Boolean  = roles.contains(Roles.ROLE_ADMIN)


