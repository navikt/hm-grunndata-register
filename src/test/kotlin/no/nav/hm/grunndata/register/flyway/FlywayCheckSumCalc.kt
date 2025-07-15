package no.nav.hm.grunndata.register.flyway


import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.flywaydb.core.internal.resolver.ChecksumCalculator
import org.flywaydb.core.internal.resource.filesystem.FileSystemResource
import org.junit.jupiter.api.Test
import java.nio.charset.Charset

@MicronautTest
class FlywayCheckSumCalcTest {


    @Test
    fun `test checksums`() {
        val file = FileSystemResource(null,"src/main/resources/db/V2_0__baseline.sql", Charset.forName("UTF-8"), false)
        val checksums = ChecksumCalculator.calculate(file)
        println(checksums)
    }
}