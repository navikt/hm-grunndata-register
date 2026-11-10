package no.nav.hm.grunndata.register.techlabel

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Test

class TechLabelServiceTest {
    @Test
    fun `unmapped and legacy blank ISO22 codes do not appear in category lookups`() {
        val repository = mockk<TechLabelRegistrationRepository>()
        fun label(code: String?) = TechLabelRegistration(
            label = "Height", isoCode = "09070601", isoCode22 = code,
            type = TechLabelType.N, unit = "cm", sort = 1, definition = null,
        )
        val mapped = label("090706")
        every { repository.findAll() } returns flowOf(
            label(null), label(""), label("   "), mapped,
        )
        val service = TechLabelService(repository)

        service.fetchAllLabels22().keys shouldBe setOf("090706")
        service.fetchLabelsByIsoCode22("09070602").map { it.id } shouldBe listOf(mapped.id)
        service.fetchLabelsByIsoCode22("12345678") shouldBe emptyList()
    }
}
