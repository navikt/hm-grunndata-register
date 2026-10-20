package no.nav.hm.grunndata.register.iso

import io.micronaut.security.authentication.Authentication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.iso.v22.IsoMapService
import no.nav.hm.grunndata.register.part.PartDraftWithDTO
import no.nav.hm.grunndata.register.part.PartService
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.series.SeriesDraftWithDTO
import no.nav.hm.grunndata.register.series.SeriesRegistrationCommonController
import no.nav.hm.grunndata.register.series.SeriesRegistrationService
import org.junit.jupiter.api.Test
import java.util.UUID

class BlankIso22DraftTest {
    private val mappings = mockk<IsoMapService>()
    private val series = mockk<SeriesRegistrationService>()
    private val supplierId = UUID.randomUUID()
    private val auth = Authentication.build("supplier", mapOf("supplierId" to supplierId.toString()))
    private val code16 = "09070601"
    private val code22 = "09070602"

    @Test
    fun `part drafts treat blank ISO22 as absent for code and version selection`() = runBlocking {
        every { mappings.mapIso16To22Lvl4(code16) } returns code22
        coEvery { series.save(any()) } answers { firstArg() }
        coEvery { series.approveSeriesAndVariants(any(), any()) } returns mockk(relaxed = true)
        val products = mockk<ProductRegistrationService>()
        coEvery { products.save(any()) } answers { firstArg() }
        val service = PartService(series, products, mockk(), mockk(), SeriesIsoMapper(mappings))

        listOf("", "   ").forEach { blank ->
            val draft = PartDraftWithDTO(
                title = "Part", isoCategory = code16, isoCategory22 = blank,
                levArtNr = UUID.randomUUID().toString(), sparePart = true, supplierId = supplierId,
            )
            service.createDraftWith(auth, draft)
            service.createDraftWithAndApprove(auth, draft)
        }

        coVerify(exactly = 4) {
            series.save(match { it.isoCategory == code16 && it.isoCategory22 == code22 })
        }
    }

    @Test
    fun `series drafts map ISO16 when ISO22 is blank`() = runBlocking {
        every { mappings.mapIso16To22Lvl4(code16) } returns code22
        val mapped = SeriesDraftWithDTO("Series", code16, code22)
        coEvery { series.createDraftWith(supplierId, auth, mapped) } returns mockk(relaxed = true)
        val controller = SeriesRegistrationCommonController(series, mockk(), mockk(), mappings)

        listOf("", "   ").forEach { blank ->
            controller.draftSeriesWith(supplierId, mapped.copy(isoCategory22 = blank), auth)
        }

        coVerify(exactly = 2) { series.createDraftWith(supplierId, auth, mapped) }
    }
}
