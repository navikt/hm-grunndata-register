package no.nav.hm.grunndata.register.iso

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.hm.grunndata.register.iso.v22.IsoMapService
import no.nav.hm.grunndata.register.series.SeriesDataDTO
import no.nav.hm.grunndata.register.series.SeriesRegistration
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.slf4j.LoggerFactory
import java.util.UUID

class SeriesIsoMapperTest {
    private val mappings = mockk<IsoMapService>()
    private val mapper = SeriesIsoMapper(mappings)
    private val original = SeriesRegistration(
        id = UUID.randomUUID(), supplierId = UUID.randomUUID(), title = "Test", text = "",
        isoCategory = "04360901", isoCategory22 = "04360902", seriesData = SeriesDataDTO(),
    )

    @Test
    fun `maintains both categories in either direction`() {
        every { mappings.mapIso22To16Lvl4("12345678") } returns "87654321"
        every { mappings.mapIso16To22Lvl4("87654321") } returns "12345678"
        val changed = mapper.update(original, "12345678", IsoVersion.v22)
        changed.isoCategory shouldBe "87654321"
        changed.isoCategory22 shouldBe "12345678"
        mapper.update(original, "87654321", IsoVersion.v16) shouldBe changed
        mapper.hasWarning(changed, IsoVersion.v22) shouldBe false
    }

    @Test
    fun `missing mapping retains existing counterpart and exposes warning`() {
        every { mappings.mapIso22To16Lvl4("12345678") } returns null
        every { mappings.mapIso16To22Lvl4("87654321") } returns null
        val changed = mapper.update(original, "12345678", IsoVersion.v22)
        changed.isoCategory shouldBe original.isoCategory
        changed.isoCategory22 shouldBe "12345678"
        mapper.hasWarning(changed, IsoVersion.v22) shouldBe true
        val changed16 = mapper.update(original, "87654321", IsoVersion.v16)
        changed16.isoCategory22 shouldBe original.isoCategory22
        mapper.hasWarning(changed16, IsoVersion.v16) shouldBe true
    }

    @ParameterizedTest
    @EnumSource(IsoVersion::class)
    fun `missing mapping sanitizes log output without changing category values`(version: IsoVersion) {
        val code = "invalid\r\nERROR forged-event"
        every { mappings.mapIso16To22Lvl4(code) } returns null
        every { mappings.mapIso22To16Lvl4(code) } returns null
        val logger = LoggerFactory.getLogger(SeriesIsoMapper::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)

        try {
            val changed = mapper.update(original, code, version)

            changed.isoCategory shouldBe if (version == IsoVersion.v16) code else original.isoCategory
            changed.isoCategory22 shouldBe if (version == IsoVersion.v22) code else original.isoCategory22
            appender.list.single().formattedMessage shouldBe
                "Cannot maintain ISO counterpart for $version category invalid__ERROR forged-event; retaining existing counterpart"
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
    }
}
