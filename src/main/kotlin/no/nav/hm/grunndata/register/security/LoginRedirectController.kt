package no.nav.hm.grunndata.register.security

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.swagger.v3.oas.annotations.Hidden

@Controller("/")
@Hidden
class LoginRedirectController {

    @Get("/login-failure")
    fun loginFailure() : HttpResponse<String> = HttpResponse.unauthorized()

    @Get("/login-success")
    fun loginSuccess(): HttpResponse<String> = HttpResponse.ok()

}
