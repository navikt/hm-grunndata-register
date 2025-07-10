package no.nav.hm.grunndata.register.productagreement

import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.micronaut.leaderelection.LeaderOnly

@Singleton
open class DetectDuplicateHmsNrProductAgreementsScheduler(private val productAgreementRepository: ProductAgreementRegistrationRepository) {

    @LeaderOnly
    @Scheduled(cron = "* 55 2 * * *")
    open fun detectDuplicateHmsNrWithDifferentSupplierRef() {
        runBlocking {
            val productAgreements = productAgreementRepository.findSupplierRefChangedSameHmsArtNr()
            if (productAgreements.size > 1) {
                LOG.error("Found ${productAgreements.size} product agreements with the same HMS art nr but different supplier references. This is not allowed.")
                productAgreements.forEach { pa ->
                    LOG.error("Product Agreement ID: ${pa.id}, HMS Art Nr: ${pa.hmsArtNr}, Supplier Ref: ${pa.supplierRef}, Agreement ID: ${pa.agreementId}")
                }
            }
        }
    }
    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(DetectDuplicateHmsNrProductAgreementsScheduler::class.java)
    }
}