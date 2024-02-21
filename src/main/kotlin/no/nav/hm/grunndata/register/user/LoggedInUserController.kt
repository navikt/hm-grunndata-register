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
    fun getLoggedInUser(authentication: Authentication): RegistrationAuthentication = RegistrationAuthentication(
        name = authentication.name,
        attributes = authentication.attributes
    )
    
}

data class RegistrationAuthentication (
    val name: String,
    val attributes: Map<String, Any>

): Authentication {
    override fun getName(): String = name

    override fun getAttributes(): Map<String, Any> = attributes

}