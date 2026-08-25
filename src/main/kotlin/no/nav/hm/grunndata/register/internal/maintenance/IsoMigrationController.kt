package no.nav.hm.grunndata.register.internal.maintenance

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import no.nav.hm.grunndata.register.iso.IsoCategoryService
import no.nav.hm.grunndata.register.iso.v22.Iso16TreeMigrate
import no.nav.hm.grunndata.register.iso.v22.Iso22Repository
import no.nav.hm.grunndata.register.iso.v22.Iso22Service
import no.nav.hm.grunndata.register.iso.v22.IsoMap
import no.nav.hm.grunndata.register.iso.v22.IsoMapEnum
import no.nav.hm.grunndata.register.iso.v22.IsoMapResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.collections.forEach

@Hidden
@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/iso/migration")
class IsoMigrationController(private val iso22Service: Iso22Service,
                             private val iso16TreeMigrate: Iso16TreeMigrate) {

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(IsoMigrationController::class.java)
    }

    @Get("/migrate")
    fun migrateIso16To22(): List<IsoMapResult> {
        val isoMaps = iso16TreeMigrate.migrateIso16Tree()
        isoMaps.forEach {
            if (!it.code22.isNullOrEmpty()) {
                iso22Service.lookUp22Code(it.code22) ?: throw Exception("Could not find iso22 code: ${it.code22} mapped from iso16 code ${it.code16}")
            }
            else {
                LOG.error("Could not find iso16 code: ${it.code16} mapped to iso22 code: ${it.code22} with enum: ${it.isoMap?.mapEnum}")
            }
        }
        return isoMaps
    }
}

