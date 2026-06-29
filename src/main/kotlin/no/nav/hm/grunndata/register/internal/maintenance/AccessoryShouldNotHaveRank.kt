package no.nav.hm.grunndata.register.internal.maintenance

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Hidden
@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/fix/accessory/shouldNotHaveRank")
class AccessoryShouldNotHaveRank(private val productAgreementRegistrationRepository: ProductAgreementRegistrationRepository,
                                 private val productRegistrationService: ProductRegistrationService) {

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(AccessoryShouldNotHaveRank::class.java)
    }

    @Put("/")
    suspend fun fixAccessories() {
        val accessories = productAgreementRegistrationRepository.findByMainProductAndRankLessThan(mainProduct = false, rank=99)
        LOG.info("Accessory agreements size: {}", accessories.size)
        var count = 0
        // set rank to 99
        accessories.forEach {
            productAgreementRegistrationRepository.update(it.copy(rank = 99))
            count++
        }
        LOG.info("Updated rank for $count product agreements")
        count = 0;
        accessories.map { it.productId }.toSet().forEach { productId ->
            productRegistrationService.findById(productId)?.let {
                LOG.info("Updating accessory product id {} : {}", it.id,  it.articleName)
                productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(it, isUpdate = true)
                count++
            }
        }
        LOG.info("Sent $count product accessories")
    }
}