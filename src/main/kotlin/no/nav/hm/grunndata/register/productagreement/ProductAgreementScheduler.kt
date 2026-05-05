package no.nav.hm.grunndata.register.productagreement

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AgreementStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.agreement.AgreementExpiration
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationService
import no.nav.hm.micronaut.leaderelection.LeaderOnly
import org.slf4j.LoggerFactory

@Singleton
@Requires(property = "schedulers.enabled", value = "true")
open class ProductAgreementScheduler(
    private val productAgreementRegistrationService: ProductAgreementRegistrationService,
    private val agreementRegistrationService: AgreementRegistrationService,
    private val agreementExpiration: AgreementExpiration
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(ProductAgreementScheduler::class.java)
    }

    @LeaderOnly
    @Scheduled(cron = "0 0 3 * * *")
    open fun deactivateProductAgreementsThatAreExpired() {
        LOG.info("Running product agreement deactivation scheduler")
        runBlocking {
            productAgreementRegistrationService.deactivateExpiredProductAgreements()
        }
    }

    @LeaderOnly
    @Scheduled(cron = "0 */30 * * * *")
    open fun activateProductAgreements() {
        LOG.info("Running product agreement activation scheduler")
        runBlocking {
            val toBePublished = productAgreementRegistrationService.findByStatusAndPublishedBeforeAndExpiredAfter(
                ProductAgreementStatus.INACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
            )
            LOG.info("Found ${toBePublished.size} product agreements to be published")
            toBePublished.forEach {
                val agreement = agreementRegistrationService.findById(it.agreementId)!!
                if (agreement.agreementStatus == AgreementStatus.ACTIVE && agreement.draftStatus == DraftStatus.DONE) {
                    productAgreementRegistrationService.saveAndCreateEvent(
                        it.copy(
                            status = ProductAgreementStatus.ACTIVE,
                            updated = LocalDateTime.now(),
                            updatedByUser = "system-publish",
                            updatedBy = REGISTER
                        ),
                        isUpdate = true
                    )
                }
                else {
                    productAgreementRegistrationService.saveAndCreateEvent(
                        it.copy(
                            status = ProductAgreementStatus.INACTIVE,
                            expired = agreement.expired,
                            updated = LocalDateTime.now()
                        ), isUpdate = true
                    )
                }
            }
        }
    }

    @LeaderOnly
    @Scheduled(cron = "0 1 15 * *")
    open fun deactivateProductExpiredActiveAgreements() {
        LOG.info("Running product agreement deactivate scheduler for products that are expired")
        runBlocking {
            productAgreementRegistrationService.deactivateProductExpiredActiveAgreements()
        }
    }

}