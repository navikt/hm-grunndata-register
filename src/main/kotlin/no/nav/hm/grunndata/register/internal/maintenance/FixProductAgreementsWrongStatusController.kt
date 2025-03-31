package no.nav.hm.grunndata.register.internal.maintenance

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import java.time.LocalDateTime
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationService
import org.slf4j.LoggerFactory

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/fix/product-agreements-wrong-status")
@Hidden
class FixProductAgreementsWrongStatusController(private val productAgreementRegistrationService: ProductAgreementRegistrationService) {


    @Put("/")
    suspend fun fixProductAgreementsWrongStatus() {
        val pags = productAgreementRegistrationService.findBystatusAndPublishedAfter(ProductAgreementStatus.ACTIVE, LocalDateTime.now())
        LOG.info("Got pags ${pags.size} that are active and published after now")
            pags.forEach {
                productAgreementRegistrationService.saveAndCreateEvent(
                    it.copy(
                        status = ProductAgreementStatus.INACTIVE,
                        updated = LocalDateTime.now(),
                        updatedBy = REGISTER,
                        updatedByUser = "system-fix",
                    ), true
                )
            }

    }

    companion object {
        private val LOG = LoggerFactory.getLogger(FixProductAgreementsWrongStatusController::class.java)
    }
}