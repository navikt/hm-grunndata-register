package no.nav.hm.grunndata.register.accessory

import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import java.util.UUID
import no.nav.hm.grunndata.register.product.ProductDTOMapper
import no.nav.hm.grunndata.register.product.ProductRegistrationDTOV2
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory
import io.swagger.v3.oas.annotations.tags.Tag

@Secured(Roles.ROLE_ADMIN, Roles.ROLE_HMS)
@Controller(AccessoryConnectController.API_V1_ACCESSORY)
@Tag(name = "Accessory CompatibleWith")
class AccessoryConnectController(private val compatibleWithFinder: CompatibleWithFinder,
                                 private val productRegistrationService: ProductRegistrationService,
                                 private val productDTOMapper: ProductDTOMapper) {

    @Get("/variants/{hmsNr}")
    suspend fun findCompatibleWithProductsVariants(hmsNr: String) = compatibleWithFinder.findCompatibleWith(hmsNr, true)


    @Put("/{id}/compatibleWith")
    suspend fun connectProductAndVariants(@Body compatibleWithDTO: CompatibleWithDTO, id: UUID): ProductRegistrationDTOV2 {
        val product = productRegistrationService.findById(id) ?: throw IllegalArgumentException("Product $id not found")
        if (!(product.accessory or product.sparePart))
            throw IllegalArgumentException("Product $id is not an accessory or spare part")
        LOG.info("Connect product $id with $compatibleWithDTO")
        val connected = compatibleWithFinder.connectWith(compatibleWithDTO, product)
        return productDTOMapper.toDTOV2(connected)
    }

    companion object {
        const val API_V1_ACCESSORY = "/api/v1/accessory"
        private val LOG = LoggerFactory.getLogger(AccessoryConnectController::class.java)
    }

}

data class CompatibleWithDTO(
    val seriesIds: Set<UUID> = emptySet(),
    val productIds: Set<UUID> = emptySet()
)