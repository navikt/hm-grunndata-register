package no.nav.hm.grunndata.register.compatiblewith

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import org.slf4j.LoggerFactory

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/compatible")
@Hidden
class CompatibleProductController(
    private val compatibleWithConnecter: CompatibleWithConnecter) {

    @Post("/connect/{hmsNr}")
    suspend fun connect(hmsNr: String, @QueryValue(defaultValue = "false") reconnect: Boolean) {
        compatibleWithConnecter.connectWithHmsNr(hmsNr, reconnect)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CompatibleProductController::class.java)
    }

    @Post("/catalog/connect/orderref/{orderRef}")
    suspend fun connectWithCatalog(orderRef: String, @QueryValue(defaultValue = "false") reconnect: Boolean) {
        LOG.info("connectWithCatalog: $orderRef")
        compatibleWithConnecter.connectCatalogOrderRef(orderRef, reconnect)
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