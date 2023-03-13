package no.nav.hm.grunndata.register.security

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get

@Controller("/login-failure")
class LoginFailureController {

    @Get("/")
    fun loginFailure() : HttpResponse<String> = HttpResponse.unauthorized()

}
