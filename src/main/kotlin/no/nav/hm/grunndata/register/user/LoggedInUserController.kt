package no.nav.hm.grunndata.register.user

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import java.security.Principal

@Secured(SecurityRule.IS_AUTHENTICATED)
@Controller("/user/authenticaed")
class LoggedInUserController {

    @Get("/")
    fun userLoggedIn(principal: Principal): String = principal.name

}