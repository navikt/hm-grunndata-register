package no.nav.hm.grunndata.register.product

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.exceptions.DataAccessException
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.jpa.criteria.impl.expression.LiteralExpression
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.RequestBean
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.runtime.where
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.supplierId
import org.slf4j.LoggerFactory

@Secured(Roles.ROLE_SUPPLIER)
@Controller(ProductRegistrationApiController.API_V1_PRODUCT_REGISTRATIONS)
@Tag(name = "Vendor Product")
class ProductRegistrationApiController(
    private val productRegistrationService: ProductRegistrationService,
    private val productDTOMapper: ProductDTOMapper,
) {
    companion object {
        const val API_V1_PRODUCT_REGISTRATIONS = "/vendor/api/v1/product/registrations"
        private val LOG = LoggerFactory.getLogger(ProductRegistrationApiController::class.java)
    }

    @Get("/series/{seriesUUID}")
    suspend fun findBySeriesUUIDAndSupplierId(
        seriesUUID: UUID,
        authentication: Authentication,
    ): List<ProductRegistrationDTO> =
        productRegistrationService.findBySeriesUUIDAndSupplierId(seriesUUID, authentication.supplierId())
            .sortedBy { it.created }.map { productDTOMapper.toDTO(it) }

    @Get("/")
    suspend fun findProducts(
        @RequestBean criteria: ProductRegistrationCriteria,
        pageable: Pageable,
        authentication: Authentication,
    ): Page<ProductRegistrationDTO> = productRegistrationService
        .findAll(buildCriteriaSpec(criteria, authentication.supplierId()), pageable)
        .mapSuspend { productDTOMapper.toDTO(it) }

    private fun buildCriteriaSpec(
        criteria: ProductRegistrationCriteria,
        supplierId: UUID,
    ): PredicateSpecification<ProductRegistration>? =
        if (criteria.isNotEmpty()) {
            where {
                root[ProductRegistration::supplierId] eq supplierId
                criteria.supplierRef?.let { root[ProductRegistration::supplierRef] eq it }
                criteria.seriesUUID?.let { root[ProductRegistration::seriesUUID] eq it }
                criteria.hmsArtNr?.let { root[ProductRegistration::hmsArtNr] eq it }
                criteria.draft?.let { root[ProductRegistration::draftStatus] eq it }
                criteria.registrationStatus?.let { statusList ->
                    root[ProductRegistration::registrationStatus] inList statusList
                }
                criteria.title?.let { root[ProductRegistration::title] like LiteralExpression("%$it%") }
            }
        } else null

    @Get("/v2/{id}")
    suspend fun getProductByIdV2(
        id: UUID,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTOV2> =
        productRegistrationService.findByIdAndSupplierId(id, authentication.supplierId())
            ?.let { HttpResponse.ok(productDTOMapper.toDTOV2(it)) } ?: HttpResponse.notFound()

    @Put("/v2/{id}")
    suspend fun updateProductV2(
        @Body registrationDTO: UpdateProductRegistrationDTO,
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTO> {
        try {
            val dto =
                productDTOMapper.toDTO(productRegistrationService.updateProduct(registrationDTO, id, authentication))
            return HttpResponse.ok(dto)
        } catch (dataAccessException: DataAccessException) {
            LOG.error("Got exception while updating product", dataAccessException)
            throw BadRequestException(
                dataAccessException.message ?: "Got exception while updating product $id",
            )
        } catch (e: Exception) {
            LOG.error("Got exception while updating product", e)
            throw BadRequestException("Got exception while updating product $id")
        }
    }

    @Post("/draftWithV3/{seriesUUID}")
    suspend fun createDraft(
        @PathVariable seriesUUID: UUID,
        @Body draftVariant: DraftVariantDTO,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTO> = try {
        val variant = productRegistrationService.createDraft(seriesUUID, draftVariant, authentication)
        HttpResponse.ok(productDTOMapper.toDTO(variant))
    } catch (dataAccessException: DataAccessException) {
        LOG.error("Got exception while updating product", dataAccessException)
        throw BadRequestException(
            dataAccessException.message ?: "Got exception while creating product",
        )
    } catch (e: Exception) {
        LOG.error("Got exception while updating product", e)
        throw BadRequestException("Got exception while creating product")
    }

    @Put("/to-expired/{id}")
    suspend fun setPublishedProductToInactive(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTO> {
        val updated =
            productRegistrationService.updateRegistrationStatus(id, authentication, RegistrationStatus.INACTIVE)
        return HttpResponse.ok(productDTOMapper.toDTO(updated))
    }

    @Put("/to-active/{id}")
    suspend fun setPublishedProductToActive(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTO> {
        val updated = productRegistrationService.updateRegistrationStatus(id, authentication, RegistrationStatus.ACTIVE)
        return HttpResponse.ok(productDTOMapper.toDTO(updated))
    }

    @Delete("/delete")
    suspend fun deleteProducts(
        @Body ids: List<UUID>,
        authentication: Authentication,
    ): HttpResponse<List<ProductRegistrationDTO>> {
        val updated = productRegistrationService.setDeletedStatus(ids, authentication)
        return HttpResponse.ok(updated.map { productDTOMapper.toDTO(it) })
    }

    @Delete("/draft/delete")
    suspend fun deleteDraftVariants(
        @Body ids: List<UUID>,
        authentication: Authentication,
    ): HttpResponse<List<ProductRegistrationDTO>> {
        productRegistrationService.deleteDraftVariants(ids, authentication)
        return HttpResponse.ok(emptyList())
    }
}

@Introspected
data class ProductRegistrationCriteria(
    val supplierId: UUID? = null,
    val supplierRef: String? = null,
    val seriesUUID: UUID? = null,
    val hmsArtNr: String? = null,
    val draft: DraftStatus? = null,
    val registrationStatus: List<RegistrationStatus>? = null,
    val title: String? = null,
) {
    fun isNotEmpty(): Boolean = supplierId != null || supplierRef != null || seriesUUID != null || hmsArtNr != null ||
            draft != null || registrationStatus != null || title != null
}

data class DraftVariantDTO(val articleName: String, val supplierRef: String)

fun Authentication.isSupplier(): Boolean = roles.contains(Roles.ROLE_SUPPLIER)