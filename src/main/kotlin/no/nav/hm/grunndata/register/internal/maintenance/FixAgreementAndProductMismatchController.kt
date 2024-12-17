package no.nav.hm.grunndata.register.internal.maintenance

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationRepository
import org.slf4j.LoggerFactory

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/fix/agreement-product-mismatch")
@Hidden
class FixAgreementAndProductMismatchController(private val productRegistrationRepository: ProductRegistrationRepository,
                                               private val productRegistrationService: ProductRegistrationService,
                                               private val productAgreementRegistrationRepository: ProductAgreementRegistrationRepository) {

    @Post("/")
    suspend fun fixAgreementAndProductMisMatchForSparePartAndAccessory() {
        val products =  productRegistrationRepository.findProductThatDoesNotMatchAgreementSparePartAccessory()
        LOG.info("Got ${products.size} products that does not match agreement and spare part/accessory")
        products.forEach {
            LOG.info("Fixing product ${it.id} for supplier ${it.supplierId}")
            val pag = productAgreementRegistrationRepository.findBySupplierIdAndSupplierRef(it.supplierId, it.supplierRef).first()
            productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                it.copy(
                    accessory = pag.accessory,
                    sparePart = pag.sparePart
                ), isUpdate = true
            )
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(FixAgreementAndProductMismatchController::class.java)
    }
}