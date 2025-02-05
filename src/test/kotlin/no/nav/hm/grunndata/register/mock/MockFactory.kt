package no.nav.hm.grunndata.register.mock

import io.micronaut.context.annotation.Factory
import io.micronaut.test.annotation.MockBean
import io.mockk.mockk
import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.gdb.GdbApiClient
import no.nav.hm.rapids_rivers.micronaut.RapidPushService


@Factory
class MockFactory {

    @Singleton
    fun mockGdbClient(): GdbApiClient = mockk(relaxed = true)

    @Singleton
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)
}