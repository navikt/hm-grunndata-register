package no.nav.hm.grunndata.register.part

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
import no.nav.hm.grunndata.register.accessory.CompatibleWithFinder
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

@Secured(Roles.ROLE_HMS, Roles.ROLE_ADMIN, Roles.ROLE_SUPPLIER)
@Controller(PartApiCommonController.API_V1_PART_REGISTRATIONS)
@Tag(name = "Common API for Parts")
class PartApiCommonController(
    private val productRegistrationService: ProductRegistrationService,
    private val seriesRegistrationService: SeriesRegistrationService,
    private val partService: PartService,
    private val productDTOMapper: ProductDTOMapper,
    private val compatibleWithFinder: CompatibleWithFinder,
) {
    companion object {
        const val API_V1_PART_REGISTRATIONS = "/common/api/v1/part"
        private val LOG = LoggerFactory.getLogger(PartApiCommonController::class.java)
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
                root[ProductRegistration::supplierId] eq authentication.supplierId()
            }

            criteria.supplierRef?.let { root[ProductRegistration::supplierRef] eq it }
            criteria.hmsArtNr?.let { root[ProductRegistration::hmsArtNr] eq it }
            criteria.title?.let { criteriaBuilder.lower(root[ProductRegistration::articleName]) like LiteralExpression("%${it.lowercase()}%") }
            or {
                root[ProductRegistration::accessory] eq true
                root[ProductRegistration::sparePart] eq true
            }
        }


    @Get("/series/{id}")
    suspend fun getPartsForSeriesId(
        id: UUID,
        authentication: Authentication
    ): List<ProductRegistrationDTOV2> {

        if (authentication.isSupplier()) {
            return productRegistrationService.findAccessoryOrSparePartCombatibleWithSeriesIdAndSupplierId(
                id,
                authentication.supplierId()
            )
                .map { productDTOMapper.toDTOV2(it) }
        } else {
            return productRegistrationService.findAccessoryOrSparePartCombatibleWithSeriesId(id)
                .map { productDTOMapper.toDTOV2(it) }
        }

    }


    @Get("/hmsNr/{hmsNr}")
    suspend fun findByHmsNr(
        hmsNr: String,
        authentication: Authentication
    ): ProductRegistrationDTOV2? {

        if (authentication.isSupplier()) {
            val product =
                productRegistrationService.findByExactHmsArtNrAndSupplierId(hmsNr, authentication.supplierId())
            return product?.let { productDTOMapper.toDTOV2(it) }
        } else {
            val product = productRegistrationService.findByExactHmsArtNr(hmsNr)
            return product?.let { productDTOMapper.toDTOV2(it) }
        }

    }

    @Get("/variant-id/{variantIdentifier}")
    suspend fun findPartByVariantIdentifier(
        @PathVariable variantIdentifier: String,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTOV2> {
        val variant = productRegistrationService.findPartByHmsArtNr(variantIdentifier, authentication)
            ?: productRegistrationService.findPartBySupplierRef(variantIdentifier, authentication)

        return variant?.let { HttpResponse.ok(productDTOMapper.toDTOV2(it)) }
            ?: HttpResponse.notFound()
    }

    @Get("/series-variants/{seriesUUID}")
    suspend fun findVariantsBySeriesUUID(
        seriesUUID: UUID,
        authentication: Authentication
    ): List<ProductRegistrationDTOV2> {

        return if (authentication.isSupplier()) {
            productRegistrationService.findBySeriesUUIDAndSupplierId(
                seriesUUID,
                authentication.supplierId()
            ).sortedBy { it.created }.map { productDTOMapper.toDTOV2(it) }

        } else {
            productRegistrationService.findAllBySeriesUuid(seriesUUID).sortedBy { it.created }
                .map { productDTOMapper.toDTOV2(it) }
        }
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

    @Put("/{id}/compatibleWith")
    suspend fun connectProductAndVariants(
        @Body compatibleWithDTO: CompatibleWithDTO,
        id: UUID,
        authentication: Authentication
    ): ProductRegistrationDTOV2 {
        if (authentication.isSupplier()) {
            val product = productRegistrationService.findByIdAndSupplierId(id, authentication.supplierId())
                ?: throw IllegalArgumentException("Product $id not found for supplier ${authentication.supplierId()}")
            if (!(product.accessory or product.sparePart))
                throw IllegalArgumentException("Product $id is not an accessory or spare part")
            LOG.info("Connect product $id with $compatibleWithDTO")
            val connected = compatibleWithFinder.connectWith(compatibleWithDTO, product)
            return productDTOMapper.toDTOV2(connected)
        } else {
            val product = productRegistrationService.findById(id)
                ?: throw IllegalArgumentException("Product $id not found")
            if (!(product.accessory or product.sparePart))
                throw IllegalArgumentException("Product $id is not an accessory or spare part")
            LOG.info("Connect product $id with $compatibleWithDTO")
            val connected = compatibleWithFinder.connectWith(compatibleWithDTO, product)
            return productDTOMapper.toDTOV2(connected)
        }
    }

    @Get("/{id}")
    suspend fun findPartById(
        id: UUID,
        authentication: Authentication
    ): PartDTO? {
        if (authentication.isSupplier()) {
            val product = productRegistrationService.findByIdAndSupplierId(id, authentication.supplierId())
                ?: throw IllegalArgumentException("Product $id not found for supplier ${authentication.supplierId()}")
            return product?.let { productDTOMapper.toPartDTO(it) }
        } else {
            val product = productRegistrationService.findById(id)
            return product?.let { productDTOMapper.toPartDTO(it) }
        }
    }

    @Get("/variants/{hmsNr}")
    suspend fun findCompatibleWithProductsVariants(hmsNr: String) = compatibleWithFinder.findCompatibleWith(hmsNr, true)

    @Put("/{seriesId}")
    suspend fun updatePart(
        seriesId: UUID,
        @Body updatePartDto: UpdatePartDto,
        authentication: Authentication
    ): HttpResponse<Map<String, String>> {
        partService.updatePart(authentication, updatePartDto, seriesId)
        return HttpResponse.ok(mapOf("message" to "Part updated successfully"))
    }

    @Put("/approve/{id}")
    suspend fun approvePart(
        id: UUID,
        authentication: Authentication,
    ): HttpResponse<Any> {

        partService.approvePart(authentication, id)
        return HttpResponse.ok(mapOf("message" to "Part approved successfully"))
    }

}

data class CompatibleWithDTO(
    val seriesIds: Set<UUID> = emptySet(),
    val productIds: Set<UUID> = emptySet()
)

data class SuitableForKommunalTeknikerDTO(
    val suitableForKommunalTekniker: Boolean,
)

data class SuitableForBrukerpassbrukerDTO(
    val suitableForBrukerpassbruker: Boolean,
)
