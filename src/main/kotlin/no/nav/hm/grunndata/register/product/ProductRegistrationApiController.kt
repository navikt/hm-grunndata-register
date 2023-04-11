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

import no.nav.hm.grunndata.register.security.supplierId
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

@Secured(Roles.ROLE_SUPPLIER)
@Controller(ProductRegistrationApiController.API_V1_PRODUCT_REGISTRATIONS)
class ProductRegistrationApiController(private val productRegistrationRepository: ProductRegistrationRepository,
                                       private val productRegistrationHandler: ProductRegistrationHandler) {

    companion object {
        const val API_V1_PRODUCT_REGISTRATIONS = "/api/v1/product/registrations"
        private val LOG = LoggerFactory.getLogger(ProductRegistrationApiController::class.java)
    }


    @Get("/{?params*}")
    suspend fun findProducts(@QueryValue params: HashMap<String,String>?,
                             pageable: Pageable, authentication: Authentication): Page<ProductRegistrationDTO> =
        productRegistrationRepository.findAll(buildCriteriaSpec(params, authentication.supplierId()), pageable).map { it.toDTO() }


    private fun buildCriteriaSpec(params: HashMap<String, String>?, supplierId: UUID): PredicateSpecification<ProductRegistration>?
            = params?.let {
        where {
            root[ProductRegistration::supplierId] eq supplierId
            if (params.contains("supplierRef")) root[ProductRegistration::supplierRef] eq params["supplierRef"]
            if (params.contains("hmsArtNr")) root[ProductRegistration::hmsArtNr] eq params["hmsArtNr"]
            if (params.contains("draft")) root[ProductRegistration::draftStatus] eq DraftStatus.valueOf(params["draft"]!!)
        }.and { root, criteriaBuilder ->
            if (params.contains("title")) criteriaBuilder.like(root[ProductRegistration::title], params["title"]) else null }
    }

    @Get("/{id}")
    suspend fun getProductById(id: UUID, authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
        productRegistrationRepository.findByIdAndSupplierId(id, authentication.supplierId())
            ?.let {
                HttpResponse.ok(it.toDTO()) }
            ?: HttpResponse.notFound()

    @Post("/")
    suspend fun createProduct(@Body registrationDTO: ProductRegistrationDTO, authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
        if (registrationDTO.supplierId != authentication.supplierId()) {
            LOG.warn("Got unauthorized attempt for ${registrationDTO.supplierId}")
            HttpResponse.unauthorized()
        }
        else if (registrationDTO.createdByAdmin || registrationDTO.adminStatus == AdminStatus.APPROVED) HttpResponse.unauthorized()
        else
            productRegistrationRepository.findById(registrationDTO.id)?.let {
                throw BadRequestException("Product registration already exists ${registrationDTO.id}")
            } ?: run {
                val dto = productRegistrationRepository.save(registrationDTO
                    .copy(updatedByUser =  authentication.name, createdByUser = authentication.name,
                        created = LocalDateTime.now(), updated = LocalDateTime.now())
                    .toEntity()).toDTO()
                productRegistrationHandler.pushToRapidIfNotDraft(dto)
                HttpResponse.created(dto)
            }

    @Put("/{id}")
    suspend fun updateProduct(@Body registrationDTO: ProductRegistrationDTO, @PathVariable id: UUID, authentication: Authentication):
            HttpResponse<ProductRegistrationDTO> =
        if (registrationDTO.supplierId != authentication.supplierId()) HttpResponse.unauthorized()
        else productRegistrationRepository.findByIdAndSupplierId(id, registrationDTO.supplierId)
                ?.let { inDb ->
                    val dto = productRegistrationRepository.update(registrationDTO
                        .copy(id = inDb.id, created = inDb.created,
                            updatedBy = REGISTER, updatedByUser = authentication.name, createdByUser = inDb.createdByUser,
                            createdBy = inDb.createdBy, createdByAdmin = inDb.createdByAdmin, adminStatus = inDb.adminStatus,
                            adminInfo = inDb.adminInfo, updated = LocalDateTime.now())
                        .toEntity()).toDTO()
                    productRegistrationHandler.pushToRapidIfNotDraft(dto)
                    HttpResponse.ok(dto) }
                ?: run {
                    throw BadRequestException("Product does not exists $id") }

    @Delete("/{id}")
    suspend fun deleteProduct(@PathVariable id:UUID, authentication: Authentication): HttpResponse<ProductRegistrationDTO> =
        productRegistrationRepository.findByIdAndSupplierId(id, authentication.supplierId())
            ?.let {
                val deleteDTO = productRegistrationRepository.update(it
                    .copy(registrationStatus = RegistrationStatus.DELETED, updatedByUser = authentication.name))
                    .toDTO()
                HttpResponse.ok(deleteDTO)
            } ?: HttpResponse.notFound()

    @Get("/draft/{supplierRef}")
    suspend fun draftProduct(@PathVariable supplierRef: String, authentication: Authentication): HttpResponse<ProductRegistrationDTO> {
        val supplierId = authentication.supplierId()
        productRegistrationRepository.findBySupplierIdAndSupplierRef(supplierId, supplierRef)?.let {
            throw BadRequestException("$supplierId and $supplierRef already exists")
        } ?: run {
            val productId = UUID.randomUUID()
            val product = ProductData (
                seriesId = productId.toString(),
                isoCategory = "",
                attributes = Attributes(
                    shortdescription = "",
                    text = "",
                    compatible = listOf(CompatibleAttribute(hmsArtNr = "", supplierRef = ""))
                )
            )
            val registration = ProductRegistrationDTO(id = productId, title = "", articleName = "", hmsArtNr = "",
                createdBy = REGISTER, supplierId = supplierId, supplierRef = supplierRef, updatedBy = REGISTER,
                message = null, published = LocalDateTime.now(), expired = LocalDateTime.now().plusYears(10),
                productData = product, version = 0 )
            return HttpResponse.ok(registration)
        }
    }

    @Get("/template/{id}")
    suspend fun useProductTemplate(@PathVariable id: UUID, authentication: Authentication): HttpResponse<ProductRegistrationDTO> {
        val supplierId = authentication.supplierId()
        return productRegistrationRepository.findByIdAndSupplierId(id, supplierId)?.let {
            HttpResponse.ok(productRegistrationHandler.makeTemplateOf(it, authentication))
        } ?: HttpResponse.notFound()
    }
}
