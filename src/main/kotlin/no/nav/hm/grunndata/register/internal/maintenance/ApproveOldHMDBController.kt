package no.nav.hm.grunndata.register.internal.maintenance

import io.micronaut.data.runtime.criteria.get
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.runtime.where
import org.slf4j.LoggerFactory


@Hidden
@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/fix/hmdb/products")
class ApproveOldHMDBController(val productRegistrationService: ProductRegistrationService) {

    @Put("/")
    suspend fun approveOldHMDBProducts(): Int {
        val products = productRegistrationService.findAll(spec = where {
            root[ProductRegistration::adminStatus] eq AdminStatus.PENDING
            root[ProductRegistration::draftStatus] eq DraftStatus.DONE
            root[ProductRegistration::mainProduct] eq true
            root[ProductRegistration::updatedBy] eq "HMDB"
            root[ProductRegistration::updatedByUser] eq "system"
        })
        var count = 0
        products.collect {
            productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                it.copy(
                    adminStatus = AdminStatus.APPROVED,
                    updatedByUser = "approvedByHMDB",
                ), isUpdate = true
            )
            count++
        }
        LOG.info("Got product count=$count")
        return count
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ApproveOldHMDBController::class.java)
    }
}