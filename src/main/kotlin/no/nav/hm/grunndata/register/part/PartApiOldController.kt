package no.nav.hm.grunndata.register.part

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.jpa.criteria.impl.expression.LiteralExpression
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.RequestBean
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.product.ProductDTOMapper
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationDTOV2
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.product.isSupplier
import no.nav.hm.grunndata.register.product.mapSuspend
import no.nav.hm.grunndata.register.runtime.where
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.supplierId
import no.nav.hm.grunndata.register.series.SeriesRegistrationService
import org.slf4j.LoggerFactory
import java.util.UUID

@Secured(Roles.ROLE_HMS, Roles.ROLE_ADMIN)
@Controller(PartApiOldController.API_V1_PART_REGISTRATIONS)
@Tag(name = "HMS-user and admin Parts")
class PartApiOldController(
    private val partService: PartService,
    private val productRegistrationService: ProductRegistrationService,
    private val seriesRegistrationService: SeriesRegistrationService,
    private val productDTOMapper: ProductDTOMapper,
) {
    companion object {
        const val API_V1_PART_REGISTRATIONS = "/api/v1/part"
        private val LOG = LoggerFactory.getLogger(PartApiOldController::class.java)
    }


    @Post("/supplier/{supplierId}/draftWithAndPublish")
    suspend fun draftSeriesWithAndPublish(
        supplierId: UUID,
        @Body draftWith: PartDraftWithDTO,
        authentication: Authentication,
    ): HttpResponse<PartDraftResponse> {
        if (authentication.isSupplier() && authentication.supplierId() != supplierId) {
            LOG.warn("SupplierId in request does not match authenticated supplierId")
            return HttpResponse.unauthorized()
        }

        val product = partService.createDraftWithAndApprove(
            authentication,
            draftWith,
        )

        return HttpResponse.ok(
            PartDraftResponse(
                product.id
            ),
        )
    }


    @Put("/approve/{seriesId}")
    suspend fun approvePart(
        seriesUUID: UUID,
        authentication: Authentication,
    ): HttpResponse<Any> {
        val seriesToUpdate = seriesRegistrationService.findById(seriesUUID) ?: return HttpResponse.notFound()

        if (seriesToUpdate.adminStatus == AdminStatus.APPROVED) throw BadRequestException("$seriesUUID is already approved")
        if (seriesToUpdate.status == SeriesStatus.DELETED) throw BadRequestException("SeriesStatus should not be deleted")

        seriesRegistrationService.approveSeriesAndVariants(seriesToUpdate, authentication)

        LOG.info("set series to approved: $seriesUUID")
        return HttpResponse.ok()
    }


    @Post("/supplier/{supplierId}/draftWith")
    suspend fun draftSeriesWith(
        supplierId: UUID,
        @Body draftWith: PartDraftWithDTO,
        authentication: Authentication,
    ): HttpResponse<PartDraftResponse> {
        if (authentication.isSupplier() && authentication.supplierId() != supplierId) {
            LOG.warn("SupplierId in request does not match authenticated supplierId")
            return HttpResponse.unauthorized()
        }

        val product = partService.createDraftWith(
            authentication,
            draftWith,
        )

        return HttpResponse.ok(
            PartDraftResponse(
                product.id
            ),
        )
    }


    @Get("/variant-id/{variantIdentifier}")
    suspend fun findPartByVariantIdentifier(
        @PathVariable variantIdentifier: String,
        authentication: Authentication,
    ): ProductRegistrationDTOV2? {
        val variant = productRegistrationService.findPartByHmsArtNr(variantIdentifier, authentication)
            ?: productRegistrationService.findPartBySupplierRef(variantIdentifier, authentication)

        return variant?.let { productDTOMapper.toDTOV2(it) }
    }


    @Get("/old/series/{seriesUUID}")
    suspend fun findBySeriesUUIDAndSupplierIdOld(seriesUUID: UUID) =
        productRegistrationService.findAllBySeriesUuid(seriesUUID).sortedBy { it.created }

    @Get("/{id}")
    suspend fun findPartById(
        id: UUID,
    ): ProductRegistrationDTOV2? {
        val part = productRegistrationService.findById(id)
        return part?.let { productDTOMapper.toDTOV2(it) }
    }

    @Get("/v2/{id}")
    suspend fun findPartByIdV2(
        id: UUID,
    ): PartDTO? {
        val part = productRegistrationService.findById(id)
        return part?.let { productDTOMapper.toPartDTO(it) }
    }

    @Get("/")
    suspend fun findParts(
        @RequestBean criteria: ProductRegistrationHmsUserCriteria,
        pageable: Pageable,
        authentication: Authentication,
    ): Page<ProductRegistrationDTOV2> = productRegistrationService
        .findAll(buildCriteriaSpec(criteria, authentication), pageable)
        .mapSuspend { productDTOMapper.toDTOV2(it) }

    private fun buildCriteriaSpec(
        criteria: ProductRegistrationHmsUserCriteria,
        authentication: Authentication
    ): PredicateSpecification<ProductRegistration> =
        where<ProductRegistration> {

            if (authentication.isSupplier()) {
                root[ProductRegistration::supplierRef] eq authentication.supplierId()
            } else {
                criteria.supplierRef?.let { root[ProductRegistration::supplierRef] eq it }
            }

            criteria.hmsArtNr?.let { root[ProductRegistration::hmsArtNr] eq it }
            criteria.title?.let { criteriaBuilder.lower(root[ProductRegistration::articleName]) like LiteralExpression("%${it.lowercase()}%") }
            or {
                root[ProductRegistration::accessory] eq true
                root[ProductRegistration::sparePart] eq true
            }
        }

}

