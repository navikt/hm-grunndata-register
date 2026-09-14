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

    //@Test
    fun readIsoMapping() {
        val isoMaps = objectMapper.readValue(
            Iso22JsonTest::class.java.classLoader.getResource("iso/mapping.json")!!.readText(),
            Array<IsoMapping>::class.java
        ).distinct()
        var codes = mutableSetOf<String>()
        val iso22maps = isoMaps.map { map ->
            codes += map.codeMap.trim()
            IsoMap(
                code16 = map.code16.replace(" ", "").trim(),
                mapEnum = IsoMapEnum.fromCode(map.codeMap.trim()),
                code22 = map.code22.replace(" ", "").trim()
            )
        }
        println(objectMapper.writeValueAsString(iso22maps))
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

@Introspected
data class IsoMapping(
    val code16: String,
    val code22: String,
    val codeMap: String
)