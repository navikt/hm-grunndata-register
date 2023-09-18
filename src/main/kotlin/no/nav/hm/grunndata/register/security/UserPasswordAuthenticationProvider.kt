package no.nav.hm.grunndata.register.security

import io.micronaut.core.async.publisher.Publishers
import io.micronaut.http.HttpRequest
import io.micronaut.security.authentication.AuthenticationProvider
import io.micronaut.security.authentication.AuthenticationRequest
import io.micronaut.security.authentication.AuthenticationResponse
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.user.UserAttribute.SUPPLIER_ID
import no.nav.hm.grunndata.register.user.UserAttribute.USER_ID

import no.nav.hm.grunndata.register.user.UserRepository
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory


@Singleton
class UserPasswordAuthenticationProvider(private val userRepository: UserRepository): AuthenticationProvider<HttpRequest<*>> {

    companion object {
        private val LOG = LoggerFactory.getLogger(UserPasswordAuthenticationProvider::class.java)
    }
    override fun authenticate(httpRequest: HttpRequest<*>,
                              authenticationRequest: AuthenticationRequest<*, *>): Publisher<AuthenticationResponse> =
        runBlocking {
            val identity = authenticationRequest.identity.toString()
            val secret = authenticationRequest.secret.toString()
             userRepository.loginUser(identity, secret)
                 ?.let {  LOG.debug("User ${it.email} with ${it.roles} logged in ")
                     val userIdMap = mapOf(USER_ID to it.id.toString())
                     val attributes = userIdMap + it.attributes
                     Publishers.just(AuthenticationResponse.success(it.email, it.roles, attributes)) }
                 ?: run {
                     LOG.error("User login failed")
                     Publishers.just(AuthenticationResponse.failure("Login failed"))
                 }
         }

}
