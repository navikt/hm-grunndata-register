package no.nav.hm.grunndata.register.internal.maintenance

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import no.nav.hm.grunndata.register.accessory.CompatibleWithFinder
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import org.slf4j.LoggerFactory

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/compatible/products")
@Hidden
class CompatibleProductController(private val compatibleWithFinder: CompatibleWithFinder,
                                  private val productRegistrationService: ProductRegistrationService) {

    @Post("/connect")
    suspend fun connect(@QueryValue hmsNr: String) {
        productRegistrationService.findByHmsArtNr(hmsNr)?.let { product ->
            compatibleWithFinder.addCompatibleWithAttributeLink(product).let { updatedProduct ->
                productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(updatedProduct, isUpdate = true)
            }
        } ?: LOG.info("No product found for hmsNr: $hmsNr")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CompatibleProductController::class.java)
    }


}