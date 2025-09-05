package no.nav.hm.grunndata.register.accessory

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import org.slf4j.LoggerFactory

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/compatible/products")
@Hidden
class CompatibleProductController(
    private val compatibleWithConnecter: CompatibleWithConnecter) {

    @Post("/connect/{hmsNr}")
    suspend fun connect(hmsNr: String) {
        compatibleWithConnecter.connectWithHmsNr(hmsNr)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CompatibleProductController::class.java)
    }

    @Post("/catalog/connect/orderref/{orderRef}")
    suspend fun connectWithCatalog(orderRef: String) {
        LOG.info("connectWithCatalog: $orderRef")
        compatibleWithConnecter.connectCatalogOrderRef(orderRef)
    }

    @Post("/connect/orders")
    suspend fun connectAllNotConnected() {
        compatibleWithConnecter.connectAllOrdersNotConnected()
    }

    @Post("/connect/products")
    suspend fun connectAllProductsNotConnected() {
        compatibleWithConnecter.connectAllProductsNotConnected()
    }

}