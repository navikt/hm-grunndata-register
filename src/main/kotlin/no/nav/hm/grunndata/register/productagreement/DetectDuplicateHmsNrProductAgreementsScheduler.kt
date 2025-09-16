package no.nav.hm.grunndata.register.productagreement

import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.micronaut.leaderelection.LeaderOnly

@Singleton
open class DetectDuplicateHmsNrProductAgreementsScheduler(private val productAgreementRepository: ProductAgreementRegistrationRepository) {

    @LeaderOnly
    @Scheduled(cron = "0 55 2 * * *")
    open fun detectDuplicateHmsNrWithDifferentSupplierRef() {
        runBlocking {
            val productAgreements = productAgreementRepository.findSupplierRefChangedSameHmsArtNr()
            if (productAgreements.size > 1) {
                productAgreements.forEach { pa ->
                    LOG.error("Same HmsNr but different supplierRef ERROR! Product Agreement ID: ${pa.id}, has same HMS Art Nr: ${pa.hmsArtNr}, Supplier Ref: ${pa.supplierRef}, Agreement ID: ${pa.agreementId}")
                }
            }
        }
    }
    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(DetectDuplicateHmsNrProductAgreementsScheduler::class.java)
    }
}