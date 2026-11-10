package no.nav.hm.grunndata.register.logging

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class LogSanitizerTest {
    @ParameterizedTest
    @ValueSource(strings = ["\r", "\n", "\r\n", "\t", "\u000B", "\u000C", "\u0085", "\u2028", "\u2029"])
    fun `should prevent injected log lines`(separator: String) {
        val input = "normal${separator}ERROR forged-event"

        input.sanitizeForLog() shouldBe "normal${"_".repeat(separator.length)}ERROR forged-event"
    }

    @Test
    fun `should replace every ISO control character`() {
        val controls = (0x00..0x1F) + (0x7F..0x9F)
        controls.forEach { code ->
            "before${code.toChar()}after".sanitizeForLog() shouldBe "before_after"
        }
    }

    @Test
    fun `should preserve ordinary values and null representation`() {
        "Produkt \u00e6\u00f8\u00e5 \"{}\"".sanitizeForLog() shouldBe "Produkt \u00e6\u00f8\u00e5 \"{}\""
        "".sanitizeForLog() shouldBe ""
        null.sanitizeForLog() shouldBe "null"
    }
}
