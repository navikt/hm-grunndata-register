package no.nav.hm.grunndata.register.security

import io.micronaut.core.async.publisher.Publishers
import io.micronaut.http.HttpRequest
import io.micronaut.security.authentication.AuthenticationProvider
import io.micronaut.security.authentication.AuthenticationRequest
import io.micronaut.security.authentication.AuthenticationResponse
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking

import no.nav.hm.grunndata.register.api.supplier.UserRepository
import org.reactivestreams.Publisher


@Singleton
class UserPasswordAuthenticationProvider(private val userRepository: UserRepository): AuthenticationProvider {

    override fun authenticate(httpRequest: HttpRequest<*>,
                              authenticationRequest: AuthenticationRequest<*, *>): Publisher<AuthenticationResponse> =
        runBlocking {
             userRepository.loginUser(
                 authenticationRequest.identity.toString(),
                 authenticationRequest.identity.toString()
             )?.let { Publishers.just(AuthenticationResponse.success(it.email)) }
                 ?: Publishers.just(AuthenticationResponse.exception("Login failed"))
         }
}