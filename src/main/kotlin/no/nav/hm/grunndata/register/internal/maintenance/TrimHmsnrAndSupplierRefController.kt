package no.nav.hm.grunndata.register.internal.maintenance

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Hidden
@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/fix/products/trim")
class TrimHmsnrAndSupplierRefController(private val productRegistrationRepository: ProductRegistrationRepository,
                                        private val productRegistrationService: ProductRegistrationService) {


    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(TrimHmsnrAndSupplierRefController::class.java)
    }

    @Put("/")
    suspend fun trim() {
        val trimSupplierRefs = productRegistrationRepository.findBySupplierRefForTrimming().map { it.copy(supplierRef = it.supplierRef.trim())}
        LOG.info("Trimming supplierRef size: ${trimSupplierRefs.size}")
        trimSupplierRefs.forEach { product ->
            LOG.info("product ${product.supplierRef}")
            try {
                productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(product, isUpdate = true)
            }
            catch (e: Exception) {
                LOG.error("Error saving product with id: ${product.id} supplierRef ${product.supplierRef}: ${e.message}")
            }
        }
        val trimHmsArtNr = productRegistrationRepository.findByHmsnrForTrimming().map { it.copy(hmsArtNr = it.hmsArtNr?.trim()) }
        LOG.info("Trimming HMS artNr size: ${trimHmsArtNr.size}")
        trimHmsArtNr.forEach { product ->
            LOG.info("product ${product.hmsArtNr}")
            try {
                productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(product, isUpdate = true)
            }
            catch (e: Exception) {
                LOG.error("Error saving product with id: ${product.id} hmsArtNr: ${product.hmsArtNr}:  ${e.message}")
            }
        }
    }

}