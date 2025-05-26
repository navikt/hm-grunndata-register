package no.nav.hm.grunndata.register.part

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.jpa.criteria.impl.expression.LiteralExpression
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.RequestBean
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.register.product.ProductDTOMapper
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationDTOV2
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.product.isSupplier
import no.nav.hm.grunndata.register.product.mapSuspend
import no.nav.hm.grunndata.register.runtime.where
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.supplierId
import org.slf4j.LoggerFactory
import java.util.UUID

@Secured(Roles.ROLE_HMS, Roles.ROLE_ADMIN, Roles.ROLE_SUPPLIER)
@Controller(PartApiCommonController.API_V1_PART_REGISTRATIONS)
@Tag(name = "Common API for Parts")
class PartApiCommonController(
    private val productRegistrationService: ProductRegistrationService,
    private val productDTOMapper: ProductDTOMapper,
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

}
