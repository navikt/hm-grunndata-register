package no.nav.hm.grunndata.register.productagreement

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.LeaderElection
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Singleton
@Requires(property = "schedulers.enabled", value = "true")
class ProductAgreementConnectScheduler(private val leaderElection: LeaderElection,
                                       private val productRegistrationService: ProductRegistrationService,
                                       private val productAgreementRegistrationService: ProductAgreementRegistrationService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(ProductAgreementConnectScheduler::class.java)
    }

    @Scheduled(cron = "0 1 2 * * *")
    fun importAndUpdateProductAgreementConnect() {
        if (leaderElection.isLeader()) {
            LOG.info("Running product agreement connect scheduler")
            runBlocking {
                val productAgreementList = productAgreementRegistrationService.findProductAgreementWithNoConnection()
                LOG.info("Found product agreements with no connection: ${productAgreementList.size}")
                productAgreementList.forEach {
                    productRegistrationService.findBySupplierRefAndSupplierId(it.supplierRef, it.supplierId)?.let { product ->
                        LOG.info("Found product ${product.id} with supplierRef: ${it.supplierRef} and supplierId: ${it.supplierId}")
                        productAgreementRegistrationService.update(it.copy(productId = product.id, updated = LocalDateTime.now()))
                    }
                }
            }
        }
    }

}