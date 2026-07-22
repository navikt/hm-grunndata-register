package no.nav.hm.grunndata.register.internal.maintenance

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import no.nav.hm.grunndata.register.iso.v22.IsoMapper
import no.nav.hm.grunndata.register.iso.v22.Iso22Repository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Hidden
@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/iso/migration")
class IsoMigrationController(private val isoMapper: IsoMapper, private val iso22Repository: Iso22Repository) {

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(IsoMigrationController::class.java)
    }

    @Get("/")
    suspend fun migrateProducts() {
    }

}