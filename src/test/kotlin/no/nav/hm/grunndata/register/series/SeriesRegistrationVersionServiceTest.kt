package no.nav.hm.grunndata.register.series

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

@MicronautTest
class SeriesRegistrationVersionServiceTest(private val seriesRegistrationVersionService: SeriesRegistrationVersionService) {

    @Test
    fun testFindLastApprovedVersion() {
        runBlocking {
        }

    }
}