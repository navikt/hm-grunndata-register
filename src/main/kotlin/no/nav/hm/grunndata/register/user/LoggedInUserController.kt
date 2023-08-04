package no.nav.hm.grunndata.register.user

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import no.nav.hm.grunndata.register.security.Roles

@Secured(value = [Roles.ROLE_SUPPLIER, Roles.ROLE_ADMIN])
@Controller("/loggedInUser")
class LoggedInUserController {

    @Get("/")
    fun getLoggedInUser(authentication: Authentication) = authentication
    
}