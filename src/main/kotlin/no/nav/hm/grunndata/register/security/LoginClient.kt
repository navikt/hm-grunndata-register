package no.nav.hm.grunndata.register.security

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client
import io.micronaut.security.authentication.UsernamePasswordCredentials

@Client("/admreg/login")
interface LoginClient {

    @Post("/")
    fun login(@Body usernamePasswordCredentials: UsernamePasswordCredentials): HttpResponse<Any>

}