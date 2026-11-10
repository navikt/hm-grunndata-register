package no.nav.hm.grunndata.register.logging

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class LogSanitizerTest {
    @ParameterizedTest
    @ValueSource(strings = ["\r", "\n", "\r\n"])
    fun `should prevent injected log lines`(separator: String) {
        val input = "normal${separator}ERROR forged-event"

        input.sanitizeForLog() shouldBe "normal${"_".repeat(separator.length)}ERROR forged-event"
    }

    @Test
    fun `should preserve ordinary values and null representation`() {
        "Produkt \u00e6\u00f8\u00e5 \"{}\"".sanitizeForLog() shouldBe "Produkt \u00e6\u00f8\u00e5 \"{}\""
        "".sanitizeForLog() shouldBe ""
        null.sanitizeForLog() shouldBe "null"
    }
}
