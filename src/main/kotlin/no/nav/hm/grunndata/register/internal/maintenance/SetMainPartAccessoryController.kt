package no.nav.hm.grunndata.register.internal.maintenance

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationRepository
import no.nav.hm.grunndata.register.series.SeriesRegistrationService

@Hidden
@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/set")
class SetMainPartAccessoryController(private val seriesRegistrationService: SeriesRegistrationService,
                                     private val productRegistrationService: ProductRegistrationService,
                                     private val productAgreementRegistrationRepository: ProductAgreementRegistrationRepository
)  {


    @Put("/mpa")
    suspend fun setMainPartAccessory(@QueryValue hmsnr: String, @QueryValue main: Boolean, @QueryValue accessory: Boolean,
                                     @QueryValue part: Boolean) {
        LOG.info("Setting main: {}, accessory: {}, part: {} for product with hmsnr: {}", main, accessory, part, hmsnr)
        val product = productRegistrationService.findByExactHmsArtNr(hmsnr)
        if (product != null) {
            LOG.info("Found product with id: {} and seriesUUID: {}", product.id, product.seriesUUID)
            productAgreementRegistrationRepository.findByProductId(product.id).forEach { agreement ->
                    val updatedAgreement = agreement.copy(mainProduct = main, accessory = accessory, sparePart = part)
                    productAgreementRegistrationRepository.update(updatedAgreement)
            }
            seriesRegistrationService.findById(product.seriesUUID)?.let {
                    val seriesUpdated = it.copy(mainProduct = main)
                    seriesRegistrationService.saveAndCreateEventIfNotDraftAndApproved(seriesUpdated, isUpdate = true)
            }
            productRegistrationService.findAllBySeriesUuid(product.seriesUUID).forEach { p ->
                val updatedProduct = p.copy(mainProduct = main, accessory = accessory, sparePart = part)
                productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(updatedProduct, isUpdate = true)
            }
        }
    }

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(SetMainPartAccessoryController::class.java)
    }
}