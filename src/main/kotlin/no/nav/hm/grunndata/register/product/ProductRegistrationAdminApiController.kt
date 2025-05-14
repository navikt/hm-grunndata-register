package no.nav.hm.grunndata.register.product

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.exceptions.DataAccessException
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.Slice
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
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.runtime.where
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.series.SeriesGroupDTO
import org.slf4j.LoggerFactory

@Secured(Roles.ROLE_ADMIN)
@Controller(ProductRegistrationAdminApiController.API_V1_ADMIN_PRODUCT_REGISTRATIONS)
@Tag(name = "Admin Product")
class ProductRegistrationAdminApiController(
    private val productRegistrationService: ProductRegistrationService,
    private val productDTOMapper: ProductDTOMapper,
) {
    companion object {
        const val API_V1_ADMIN_PRODUCT_REGISTRATIONS = "/admin/api/v1/product/registrations"
        private val LOG = LoggerFactory.getLogger(ProductRegistrationAdminApiController::class.java)
    }

    @Get("/series/group")
    suspend fun findSeriesGroup(
        pageable: Pageable,
    ): Slice<SeriesGroupDTO> = productRegistrationService.findSeriesGroup(pageable)

    @Get("/old/series/{seriesUUID}")
    suspend fun findBySeriesUUIDAndSupplierIdOld(seriesUUID: UUID) =
        productRegistrationService.findAllBySeriesUuid(seriesUUID).sortedBy { it.created }

    @Get("/series/{seriesUUID}")
    suspend fun findBySeriesUUIDAndSupplierId(seriesUUID: UUID) =
        productRegistrationService.findAllBySeriesUuid(seriesUUID).sortedBy { it.created }
            .map { productDTOMapper.toDTOV2(it) }


    @Get("/")
    suspend fun findProducts(
        @RequestBean criteria: ProductRegistrationAdminCriteria,
        pageable: Pageable,
    ): Page<ProductRegistrationDTOV2> = productRegistrationService
        .findAll(buildCriteriaSpec(criteria), pageable)
        .mapSuspend { productDTOMapper.toDTOV2(it) }

    private fun buildCriteriaSpec(criteria: ProductRegistrationAdminCriteria): PredicateSpecification<ProductRegistration>? =
        if (criteria.isNotEmpty()) {
            where {
                criteria.supplierRef?.let { root[ProductRegistration::supplierRef] eq it }
                criteria.hmsArtNr?.let { root[ProductRegistration::hmsArtNr] eq it }
                criteria.adminStatus?.let { root[ProductRegistration::adminStatus] eq it }
                criteria.registrationStatus?.let { root[ProductRegistration::registrationStatus] eq it }
                criteria.supplierId?.let { root[ProductRegistration::supplierId] eq it }
                criteria.draft?.let { root[ProductRegistration::draftStatus] eq it }
                criteria.createdByUser?.let { root[ProductRegistration::createdByUser] eq it }
                criteria.updatedByUser?.let { root[ProductRegistration::updatedByUser] eq it }
                criteria.title?.let { root[ProductRegistration::title] like LiteralExpression("%${it}%") }
            }
        } else null

    @Get("/til-godkjenning")
    suspend fun findProductsPendingApprove(
        pageable: Pageable,
    ): Page<ProductToApproveDto> = productRegistrationService.findProductsToApprove(pageable)

    @Get("/{id}")
    suspend fun getProductById(id: UUID): HttpResponse<ProductRegistrationDTOV2> =
        productRegistrationService.findById(id)
            ?.let { HttpResponse.ok(productDTOMapper.toDTOV2(it)) } ?: HttpResponse.notFound()

    @Get("/hmsArtNr/{hmsArtNr}")
    suspend fun getProductByHmsArtNr(hmsArtNr: String): HttpResponse<ProductRegistrationDTO> =
        productRegistrationService.findByHmsArtNr(hmsArtNr)
            ?.let { HttpResponse.ok(productDTOMapper.toDTO(it)) } ?: HttpResponse.notFound()

    @Post("/draftWithV3/{seriesUUID}")
    suspend fun createDraft(
        @PathVariable seriesUUID: UUID,
        @Body draftVariant: DraftVariantDTO,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTOV2> =
        try {
            val variant = productRegistrationService.createDraft(seriesUUID, draftVariant, authentication)
            HttpResponse.ok(productDTOMapper.toDTOV2(variant))
        } catch (e: DataAccessException) {
            throw BadRequestException(e.message ?: "Error creating draft")
        } catch (e: Exception) {
            throw BadRequestException("Error creating draft")
        }

    @Put("/{id}")
    suspend fun updateProduct(
        @Body registrationDTO: UpdateProductRegistrationDTO,
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTOV2> =
        try {
            val dto = productDTOMapper.toDTOV2(
                productRegistrationService.updateProduct(
                    registrationDTO,
                    id,
                    authentication
                )
            )
            HttpResponse.ok(dto)
        } catch (dataAccessException: DataAccessException) {
            LOG.error("Got exception while updating product", dataAccessException)
            throw BadRequestException(dataAccessException.message ?: "Got exception while updating product $id")
        } catch (e: Exception) {
            LOG.error("Got exception while updating product", e)
            throw BadRequestException("Got exception while updating product $id")
        }

    @Put("/to-expired/{id}")
    suspend fun setPublishedProductToInactive(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<Any> {
        productRegistrationService.updateExpiredStatus(id, authentication, true)
        return HttpResponse.ok()
    }

    @Put("/to-active/{id}")
    suspend fun setPublishedProductToActive(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<Any> {
        productRegistrationService.updateExpiredStatus(id, authentication, false)
        return HttpResponse.ok()
    }

    @Delete("/delete")
    suspend fun deleteProducts(
        @Body ids: List<UUID>,
        authentication: Authentication,
    ): HttpResponse<List<ProductRegistrationDTOV2>> {
        val updated = productRegistrationService.setDeletedStatus(ids, authentication)
        return HttpResponse.ok(updated.map { productDTOMapper.toDTOV2(it) })
    }

    @Delete("/draft/delete")
    suspend fun deleteDraftVariants(
        @Body ids: List<UUID>,
        authentication: Authentication,
    ): HttpResponse<Any> {
        productRegistrationService.deleteDraftVariants(ids, authentication)
        return HttpResponse.ok()
    }

    @Post("/draft/variant/{id}")
    suspend fun createProductVariant(
        @PathVariable id: UUID,
        @Body draftVariant: DraftVariantDTO,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTO> {
        return try {
            productRegistrationService.createProductVariant(id, draftVariant, authentication)?.let {
                HttpResponse.ok(productDTOMapper.toDTO(it))
            } ?: HttpResponse.notFound()
        } catch (e: DataAccessException) {
            LOG.error("Got exception while creating variant ${draftVariant.supplierRef}", e)
            throw BadRequestException(e.message ?: "Error creating variant")
        } catch (e: Exception) {
            LOG.error("Got exception while creating variant ${draftVariant.supplierRef}", e)
            throw e
        }
    }
}

@Introspected
data class ProductRegistrationAdminCriteria(
    val supplierRef: String? = null,
    val hmsArtNr: String? = null,
    val adminStatus: AdminStatus? = null,
    val registrationStatus: RegistrationStatus? = null,
    val supplierId: UUID? = null,
    val draft: DraftStatus? = null,
    val createdByUser: String? = null,
    val updatedByUser: String? = null,
    val title: String? = null,
) {
    fun isNotEmpty(): Boolean = listOfNotNull(
        supplierRef, hmsArtNr, adminStatus, registrationStatus, supplierId, draft, createdByUser, updatedByUser, title
    ).isNotEmpty()
}

fun Authentication.isAdmin(): Boolean = roles.contains(Roles.ROLE_ADMIN)

fun Authentication.isHms(): Boolean = roles.contains(Roles.ROLE_HMS)
