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
        var mapped = 0
        var notMapped = 0
        LOG.info("Starting ISO migration for products")
        val isos = iso22Repository.findDistinctIsoCodes()
        isos.forEach {
            if (it.isoCategory.length>5) {
                val isoLevel3 = it.isoCategory.substring(0, 6)
                if (isoMapper.getIso16ToIso22(it.isoCategory) != null) {
                    mapped++
                } else if (isoMapper.getIso16ToIso22(isoLevel3) != null) {
                    mapped++
                } else {
                    LOG.error("${it.isoCategory} could not be mapped")
                    notMapped++
                }
            }
            else {
                LOG.error("${it.isoCategory} can not be mapped")
                notMapped++
            }
        }
        LOG.error("ISO migration finished. Total: ${isos.size} Mapped: $mapped, Not mapped: $notMapped")
    }

}