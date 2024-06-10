package no.nav.hm.grunndata.register.internal

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import no.nav.hm.grunndata.register.eventstore.BigQueryService

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal")
@Hidden
class AliveController(private val bigQueryService: BigQueryService) {

    @Get("/isAlive")
    fun alive() = "ALIVE"

    @Get("/isReady")
    fun ready() = "OK"

    @Get("/bq/check") // For testing purposes
    fun bigQuery() = bigQueryService.checkDataSet()

}
