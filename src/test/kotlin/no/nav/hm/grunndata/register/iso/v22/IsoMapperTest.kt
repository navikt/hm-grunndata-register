package no.nav.hm.grunndata.register.iso.v22

import io.kotest.matchers.nulls.shouldNotBeNull
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

@MicronautTest
class IsoMapperTest(private val mapper: IsoMapper) {

    companion object {
        private val LOG = LoggerFactory.getLogger(IsoMapperTest::class.java)
    }

    @Test
    fun readIsoMapping() {

    }
}