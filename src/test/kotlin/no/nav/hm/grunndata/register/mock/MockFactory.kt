package no.nav.hm.grunndata.register.mock

import io.micronaut.context.annotation.Factory
import io.mockk.mockk
import jakarta.inject.Singleton

import no.nav.hm.rapids_rivers.micronaut.RapidPushService


@Factory
class MockFactory {


    @Singleton
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)
}