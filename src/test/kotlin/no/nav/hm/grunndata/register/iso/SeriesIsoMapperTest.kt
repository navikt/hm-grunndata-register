package no.nav.hm.grunndata.register.iso

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.hm.grunndata.register.iso.v22.IsoMapService
import no.nav.hm.grunndata.register.series.SeriesDataDTO
import no.nav.hm.grunndata.register.series.SeriesRegistration
import org.junit.jupiter.api.Test
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
}
