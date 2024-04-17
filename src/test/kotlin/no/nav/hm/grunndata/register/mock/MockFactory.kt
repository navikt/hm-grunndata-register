package no.nav.hm.grunndata.register.mock

import io.micronaut.context.annotation.Factory
import io.mockk.mockk
import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.gdb.GdbApiClient


@Factory
class MockFactory {

    @Singleton
    fun mockGdbClient(): GdbApiClient = mockk(relaxed = true)

}