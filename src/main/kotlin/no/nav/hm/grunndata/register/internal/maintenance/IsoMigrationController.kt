package no.nav.hm.grunndata.register.internal.maintenance

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import no.nav.hm.grunndata.register.iso.IsoCategoryService
import no.nav.hm.grunndata.register.iso.v22.Iso22Repository
import no.nav.hm.grunndata.register.iso.v22.Iso22Service
import no.nav.hm.grunndata.register.iso.v22.IsoMap
import no.nav.hm.grunndata.register.iso.v22.IsoMapEnum
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.collections.forEach

@Hidden
@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/iso/migration")
class IsoMigrationController(private val iso22Service: Iso22Service,
                             private val iso22Repository: Iso22Repository) {

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(IsoMigrationController::class.java)
    }

    @Get("/dryrun")
    suspend fun migrateProducts() {
        val isosInDb = iso22Repository.findAllDistinctIso16InDb()
        val isomappings: MutableMap<List<IsoMapEnum>, Int> = mutableMapOf()
        var noMap = 0
        isosInDb.forEach { iso ->
            // get the first
            // 6 numbers.
            val isoMap = iso22Service.toIsoMap(iso.isoCategory.take(6))
            if (isoMap != null) {
                isomappings[isoMap.mapEnum] = isomappings.getOrDefault(isoMap.mapEnum, 0) + 1
            }
            else {
                noMap++
            }
        }
        println(isomappings)
        println("No mapping: $noMap")
    }

}

data class IsoMigrationResult(
    val isoCategory: String,
    val isoMap: List<IsoMap> = emptyList(),
    val isoTitle: String,
    val iso22Title: String,
    val count: Int
)