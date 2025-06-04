package no.nav.hm.grunndata.register.accessory

import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.register.product.ProductDTOMapper
import no.nav.hm.grunndata.register.product.ProductRegistrationDTOV2
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory
import java.util.UUID

@Secured(Roles.ROLE_ADMIN, Roles.ROLE_HMS)
@Controller(AccessoryCompatibleWithController.API_V1_ACCESSORY)
@Tag(name = "Accessory CompatibleWith")
class AccessoryCompatibleWithController(
    private val compatibleWithFinder: CompatibleWithFinder,
    private val productRegistrationService: ProductRegistrationService,
    private val productDTOMapper: ProductDTOMapper
) {

    companion object {
        const val API_V1_ACCESSORY = "/api/v1/accessory"
        private val LOG = LoggerFactory.getLogger(AccessoryCompatibleWithController::class.java)
    }

    @Deprecated("Use similar method in PartApiCommonController instead")
    @Get("/series-variants/{seriesUUID}")
    suspend fun findVariantsBySeriesUUID(
        @PathVariable seriesUUID: UUID,
        authentication: Authentication,
    ): List<ProductRegistrationDTOV2> {
        val variants = productRegistrationService.findAllBySeriesUuid(seriesUUID)
        return variants.map { productDTOMapper.toDTOV2(it) }
    }

    @Deprecated("Use similar method in PartApiCommonController instead")
    @Get("/variants/{hmsNr}")
    suspend fun findCompatibleWithProductsVariants(hmsNr: String) = compatibleWithFinder.findCompatibleWith(hmsNr, true)

    @Deprecated("Use similar method in PartApiCommonController instead")
    @Get("/hmsNr/{hmsNr}")
    suspend fun findByHmsNr(hmsNr: String): ProductRegistrationDTOV2? {
        val product = productRegistrationService.findByExactHmsArtNr(hmsNr)
        return product?.let { productDTOMapper.toDTOV2(it) }
    }

    @Deprecated("Use similar method in PartApiCommonController instead")
    @Get("/hmsNr/part/{hmsNr}")
    suspend fun findPartByHmsNr(
        @PathVariable hmsNr: String,
        authentication: Authentication
    ): ProductRegistrationDTOV2? {
        val product = productRegistrationService.findPartByHmsArtNr(hmsNr, authentication)
        return product?.let { productDTOMapper.toDTOV2(it) }
    }

@Deprecated("Use similar method in PartApiCommonController instead")
    @Get("/variant-id/{variantIdentifier}")
    suspend fun findProdyctByVariantIdentifier(
        @PathVariable variantIdentifier: String,
        authentication: Authentication,
    ): ProductRegistrationDTOV2? {
        val variant = productRegistrationService.findByHmsArtNr(variantIdentifier, authentication)
            ?: productRegistrationService.findBySupplierRef(variantIdentifier, authentication)

        return variant?.let { productDTOMapper.toDTOV2(it) }
    }

    @Deprecated("Use similar method in PartApiCommonController instead")
    @Get("/{id}")
    suspend fun findById(id: UUID): ProductRegistrationDTOV2? {
        val product = productRegistrationService.findById(id)
        return product?.let { productDTOMapper.toDTOV2(it) }
    }

    @Deprecated("Use similar method in PartApiAdminController instead")
    @Put("/{id}/compatibleWith")
    suspend fun connectProductAndVariants(
        @Body compatibleWithDTO: CompatibleWithDTO,
        id: UUID
    ): ProductRegistrationDTOV2 {
        val product = productRegistrationService.findById(id) ?: throw IllegalArgumentException("Product $id not found")
        if (!(product.accessory or product.sparePart))
            throw IllegalArgumentException("Product $id is not an accessory or spare part")
        LOG.info("Connect product $id with $compatibleWithDTO")
        val connected = compatibleWithFinder.connectWith(compatibleWithDTO, product)
        return productDTOMapper.toDTOV2(connected)
    }


    @Deprecated("Use similar method in PartApiAdminController instead")
    @Put("/{id}/suitableForKommunalTekniker")
    suspend fun updateSuitableForKommunalTekniker(
        @Body suitableForKommunalTeknikerDTO: SuitableForKommunalTeknikerDTO,
        id: UUID
    ): ProductRegistrationDTOV2 {
        val product = productRegistrationService.findById(id) ?: throw IllegalArgumentException("Product $id not found")
        if (!(product.accessory or product.sparePart))
            throw IllegalArgumentException("Product $id is not an accessory or spare part")
        val updated = product.copy(
            productData = product.productData.copy(
                attributes = product.productData.attributes.copy(
                    egnetForKommunalTekniker = suitableForKommunalTeknikerDTO.suitableForKommunalTekniker
                )
            )
        )
        productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(updated, isUpdate = true)
        return productDTOMapper.toDTOV2(updated)
    }

    @Deprecated("Use similar method in PartApiAdminController instead")
    @Put("/{id}/suitableForBrukerpassbruker")
    suspend fun updateSuitableForBrukerpassbruker(
        @Body suitableForBrukerpassbruker: SuitableForBrukerpassbrukerDTO,
        id: UUID
    ): ProductRegistrationDTOV2 {
        val product = productRegistrationService.findById(id) ?: throw IllegalArgumentException("Product $id not found")
        if (!(product.accessory or product.sparePart))
            throw IllegalArgumentException("Product $id is not an accessory or spare part")
        val updated = product.copy(
            productData = product.productData.copy(
                attributes = product.productData.attributes.copy(
                    egnetForBrukerpass = suitableForBrukerpassbruker.suitableForBrukerpassbruker
                )
            )
        )
        productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(updated, isUpdate = true)
        return productDTOMapper.toDTOV2(updated)
    }

    @Deprecated("Use similar method in PartApiCommonController instead")
    @Get("/series/{id}")
    suspend fun getPartsForSeriesId(id: UUID): List<ProductRegistrationDTOV2> =
        productRegistrationService.findAccessoryOrSparePartCombatibleWithSeriesId(id)
            .map { productDTOMapper.toDTOV2(it) }



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