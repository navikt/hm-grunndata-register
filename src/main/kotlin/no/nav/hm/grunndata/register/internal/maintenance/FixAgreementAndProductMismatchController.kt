package no.nav.hm.grunndata.register.internal.maintenance

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
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

    @Post("/category")
    suspend fun fixAgreementAndProductMisMatchForSparePartAndAccessory() {
        val products =  productRegistrationRepository.findProductThatDoesNotMatchAgreementSparePartAccessory()
        LOG.info("Got ${products.size} products that does not match agreement and spare part/accessory")
        products.forEach { product ->
            LOG.info("Fixing product ${product.id} for supplier ${product.supplierId}")
            val pag =
                productAgreementRegistrationRepository.findBySupplierIdAndSupplierRef(product.supplierId, product.supplierRef)
                    .firstOrNull { it.status == ProductAgreementStatus.ACTIVE }
            pag?.let {
                productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                    product.copy(
                        hmsArtNr = pag.hmsArtNr,
                        accessory = pag.accessory,
                        sparePart = pag.sparePart
                    ), isUpdate = true
                )
            }
        }
    }

    @Post("/status")
    suspend fun fixAgreementAndProductMisMatchForStatus() {
        val products =  productRegistrationRepository.findProductThatDoesNotMatchAgreementStatus()
        LOG.info("Got ${products.size} products that does not match agreement status")
        products.forEach { product ->
            LOG.info("Fixing product ${product.id} for supplier ${product.supplierId}")
            val pag =
                productAgreementRegistrationRepository.findBySupplierIdAndSupplierRef(product.supplierId, product.supplierRef)
                    .firstOrNull { it.status == ProductAgreementStatus.ACTIVE }
            pag?.let {
                productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                    product.copy(
                        registrationStatus = RegistrationStatus.ACTIVE,
                        expired = pag.expired
                    ), isUpdate = true
                )
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(FixAgreementAndProductMismatchController::class.java)
    }
}