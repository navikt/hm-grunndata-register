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

    @Test
    fun iso22JsonTest() {
        val isoLabels = objectMapper.readValue(
            Iso22JsonTest::class.java.classLoader.getResource("iso/3-level.json")!!.readText(),
            Array<IsoLabel>::class.java
        )
        val iso16s = objectMapper.readValue(Iso22JsonTest::class.java.classLoader.getResource("iso/iso16.json")!!.readText(),
            Array<IsoCategoryDTO>::class.java).associateBy { it.isoCode }
        val newCodeNotFound : MutableList<Iso22> = mutableListOf()
        val iso22s = isoLabels.map { label ->
            val cleanCode = label.code.replace(" ", "")
            val iso16 = iso16s[cleanCode]
            if (iso16 != null) {
                Iso22(
                    isoCode = cleanCode.trim(),
                    isoTitle = label.label.trim(),
                    isoText = label.text.trim(),
                    isoTranslations = IsoTranslations(
                        titleEn = iso16.isoTranslations?.titleEn,
                        textEn = iso16.isoTranslations?.textEn,
                    ),
                    searchWords = iso16.searchWords,
                    createdByUser = "system",
                    updatedByUser = "system",
                )
            } else {
                val new = Iso22(
                    isoCode = cleanCode.trim(),
                    isoTitle = label.label.trim(),
                    isoText = label.text.trim(),
                    createdByUser = "system",
                    updatedByUser = "system",
                )
                newCodeNotFound += new
                new
            }
        }
        newCodeNotFound.forEach { newCode ->
            LOG.info("New code found: $newCode")
        }
        LOG.info("Total of new codes ${newCodeNotFound.size}")
        LOG.info("Of total of ${iso22s.size}")

        println(objectMapper.writeValueAsString(iso22s))

    }

    @Test
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
        File("src/test/resources/iso/iso22-map.json").writeText(
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(iso22maps)
        )
    }

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

@Introspected
data class IsoLabel(
    val code: String,
    val label: String,
    val text: String
)

@Introspected
data class IsoMapping(
    val code16: String,
    val codeMap: String,
    val code22: String,
)