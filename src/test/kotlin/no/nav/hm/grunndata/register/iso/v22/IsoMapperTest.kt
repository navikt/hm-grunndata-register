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
    fun testIsoMapper() {
        mapper.kode16Map.shouldNotBeNull()
        var count=0
        mapper.kode16Map.sortedBy { it.kode16 }.forEach {
            if (it.kode16 != null && it.kode22 == null) {
                count++
                LOG.info("${it.kode16}  -> ${it.kode22} (${it.konv} - ${it.betydning})  ${it.isoTitle_no}")
            }
            //LOG.info("${it.kode16} ${it.titel16eng} -> ${it.kode22} (${it.konv} - ${it.betydning}) ${it.titel22eng} ${it.isoText_no}")
        }
        LOG.info("${count} -> ${mapper.kode16Map.size}")
    }
}