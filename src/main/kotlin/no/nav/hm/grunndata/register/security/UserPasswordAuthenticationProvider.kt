package no.nav.hm.grunndata.register.security

import io.micronaut.core.async.publisher.Publishers
import io.micronaut.http.HttpRequest
import io.micronaut.security.authentication.AuthenticationProvider
import io.micronaut.security.authentication.AuthenticationRequest
import io.micronaut.security.authentication.AuthenticationResponse
import jakarta.inject.Singleton
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.grunndata.register.user.UserAttribute.SUPPLIER_ID
import no.nav.hm.grunndata.register.user.UserAttribute.SUPPLIER_NAME
import no.nav.hm.grunndata.register.user.UserAttribute.USER_ID
import no.nav.hm.grunndata.register.user.UserAttribute.USER_NAME

import no.nav.hm.grunndata.register.user.UserRepository
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory


@Singleton
class UserPasswordAuthenticationProvider(
    private val userRepository: UserRepository,
    private val supplierRegistrationService: SupplierRegistrationService
) : AuthenticationProvider<HttpRequest<*>> {

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
                     val userNameMap = mapOf(USER_NAME to it.name)
                     val supplierNameMap = mapOf(SUPPLIER_NAME to supplierName(it.attributes[SUPPLIER_ID].orEmpty()))
                     val attributes = userIdMap + userNameMap + supplierNameMap + it.attributes
                     Publishers.just(AuthenticationResponse.success(it.email, it.roles, attributes)) }
                 ?: run {
                     LOG.error("User login failed")
                     Publishers.just(AuthenticationResponse.failure("Login failed"))
                 }
         }

    private suspend fun supplierName(supplierId: String) =
        runCatching {
            supplierRegistrationService.findById(UUID.fromString(supplierId))?.name
        }.getOrDefault("")
}