package no.nav.hm.grunndata.register

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Factory
class CoroutineScopeCreation {

    @Bean
    fun createCoroutineScopeBean(): CoroutineScope {
        return CoroutineScope(Dispatchers.IO)
    }
}