package no.nav.hm.grunndata.register.iso.v22

import io.micronaut.core.annotation.Introspected
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import no.nav.hm.grunndata.rapid.dto.IsoCategoryDTO
import no.nav.hm.grunndata.register.iso.IsoTranslations
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.io.File

@MicronautTest
class Iso22JsonTest(private val objectMapper: ObjectMapper) {

    companion object   {
        private val LOG = org.slf4j.LoggerFactory.getLogger(Iso22JsonTest::class.java)
    }

    @Test
    fun mapEnumTest() {
        val codes = "#*, =, C, *, ~*, ≥*, ≥, X, C+, ~, >, >*, <, C*, #, +, *<".split(",")
        codes.forEach { code ->
            LOG.info("Code found: $code")
            IsoMapEnum.fromCode(code).let {
                LOG.info("code: $code, enum: $it")
            }
        }

    }
}