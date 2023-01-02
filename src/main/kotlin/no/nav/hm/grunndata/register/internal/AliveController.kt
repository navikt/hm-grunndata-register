package no.nav.hm.grunndata.register.internal

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal")
class AliveController {

    @Get("/isAlive")
    fun alive() = "ALIVE"

    @Get("/isReady")
    fun ready() = "OK"

}