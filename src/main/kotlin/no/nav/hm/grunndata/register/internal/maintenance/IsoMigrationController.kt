package no.nav.hm.grunndata.register.internal.maintenance

import io.micronaut.http.annotation.Controller
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import no.nav.hm.grunndata.register.iso.v22.Iso16ToIso22Util
import no.nav.hm.grunndata.register.iso.v22.Iso22Service
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Hidden
@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/iso/migration")
class IsoMigrationController(private val iso22Service: Iso22Service,
                             private val iso16ToIso22Util: Iso16ToIso22Util) {

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(IsoMigrationController::class.java)
    }

}

