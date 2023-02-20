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
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.RegisterRapidPushService

import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierRepository
import no.nav.hm.grunndata.register.supplier.toDTO
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

@Secured(Roles.ROLE_ADMIN)
@Controller(ProductRegistrationAdminApiController.API_V1_ADMIN_PRODUCT_REGISTRATIONS)
class ProductRegistrationAdminApiController(private val productRegistrationRepository: ProductRegistrationRepository,
                                            private val registerRapidPushService: RegisterRapidPushService,
                                            private val supplierRepository: SupplierRepository) {

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
            if (params.contains("draft")) root[ProductRegistration::draftStatus] eq DraftStatus.valueOf(params["draft"]!!)
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

    @Get("/draft/{supplierId}")
    suspend fun draftProduct(supplierId: UUID, authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
        supplierRepository.findById(supplierId)?.let {
            val supplier = it.toDTO()
            val productId = UUID.randomUUID()
            val product = ProductDTO(id = productId, updatedBy = REGISTER, createdBy = REGISTER, title = "", status = ProductStatus.INACTIVE,
                supplier = supplier, supplierRef = productId.toString(), identifier = productId.toString(),
                seriesId = productId.toString(), isoCategory = "", attributes = mapOf(AttributeNames.articlename to "artikkelnavn",
                AttributeNames.shortdescription to "kort beskrivelse", AttributeNames.text to "en lang beskrivelse")
            )
            val registration = ProductRegistrationDTO(id = productId, supplierId= supplier.id, HMSArtNr = null,   createdBy = REGISTER,
            updatedBy = REGISTER, supplierRef = productId.toString(), message = null, title = product.title,  published = product.published,
            expired = product.expired, productDTO = product)
            HttpResponse.ok(registration)
        } ?: HttpResponse.badRequest()


    @Post("/")
    suspend fun createProduct(@Body registrationDTO: ProductRegistrationDTO, authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
            productRegistrationRepository.findById(registrationDTO.id)?.let {
                HttpResponse.badRequest()
            } ?: run {
                val dto = productRegistrationRepository.save(registrationDTO
                    .copy(createdByUser = authentication.name, updatedByUser = authentication.name, createdByAdmin = true)
                    .toEntity()).toDTO()
                if (dto.draftStatus == DraftStatus.DONE && dto.adminStatus == AdminStatus.APPROVED) {
                    registerRapidPushService.pushDTOToKafka(dto, EventName.productRegistration)
                }
                HttpResponse.created(dto)
            }

    @Put("/{id}")
    suspend fun updateProduct(@Body registrationDTO: ProductRegistrationDTO, @PathVariable id: UUID, authentication: Authentication):
            HttpResponse<ProductRegistrationDTO> =
        productRegistrationRepository.findById(id)
                ?.let {
                    val updated = registrationDTO.copy(id = it.id, created = it.created, supplierId = it.supplierId,
                        updatedByUser = authentication.name, updatedBy = REGISTER, createdBy = it.createdBy,
                        createdByAdmin = it.createdByAdmin)
                    val dto = productRegistrationRepository.update(updated.toEntity()).toDTO()
                    if (dto.draftStatus == DraftStatus.DONE && dto.adminStatus == AdminStatus.APPROVED) {
                        registerRapidPushService.pushDTOToKafka(dto, EventName.productRegistration)
                    }
                    HttpResponse.ok(dto) }
                ?: run {
                    HttpResponse.badRequest() }

    @Delete("/{id}")
    suspend fun deleteProduct(@PathVariable id:UUID, authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
        productRegistrationRepository.findById(id)
            ?.let {
                val productDTO = it.productDTO.copy(status = ProductStatus.INACTIVE, expired = LocalDateTime.now().minusMinutes(1L))
                val dto = productRegistrationRepository.update(it.copy(status= RegistrationStatus.DELETED, productDTO = productDTO)).toDTO()
                if (dto.draftStatus == DraftStatus.DONE && dto.adminStatus == AdminStatus.APPROVED) {
                    registerRapidPushService.pushDTOToKafka(dto, EventName.productRegistration)
                }
                HttpResponse.ok(dto)}
            ?: HttpResponse.notFound()

}


