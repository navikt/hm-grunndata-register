package no.nav.hm.grunndata.register.internal.maintenance

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import no.nav.hm.grunndata.register.accessory.CompatibleWithFinder
import org.slf4j.LoggerFactory

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/compatible/products")
@Hidden
class CompatibleProductController(private val compatibleWithFinder: CompatibleWithFinder) {

    @Post("/connect/{hmsNr}")
    suspend fun connect(hmsNr: String) {
        compatibleWithFinder.connectWithHmsNr(hmsNr)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CompatibleProductController::class.java)
    }

    @Post("/connect/orderref/{orderRef}")
    suspend fun connectWithOrderRef(orderRef: String) {
        compatibleWithFinder.connectWithOrderRef(orderRef)
    }

    @Post("/connect/all/notconnected")
    suspend fun connectAllNotConnected() {
        compatibleWithFinder.connectAllNotConnected()
    }

}