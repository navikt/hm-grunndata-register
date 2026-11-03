package no.nav.hm.grunndata.register.product

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.UUID

class ProductRegistrationBulkTechDataSupplierServiceTest {
    @Test
    fun `unexpected failure returns a safe message and does not stop remaining variants`() = runBlocking {
        val repository = mockk<ProductRegistrationRepository>()
        val eventHandler = mockk<ProductRegistrationEventHandler>()
        val service = ProductRegistrationService(
            repository, mockk(), eventHandler, mockk(), mockk(), mockk(), mockk(), mockk(), mockk(),
        )
        val supplierId = UUID.randomUUID()
        val failedId = UUID.randomUUID()
        val unknownId = UUID.randomUUID()
        coEvery { repository.findByIdAndSupplierId(failedId, supplierId) } throws
            IllegalStateException("Internal database details")
        coEvery { repository.findByIdAndSupplierId(unknownId, supplierId) } returns null

        val result = service.bulkUpdateSupplierTechData(
            BulkTechDataUpdateDTO(listOf(failedId, unknownId).map { VariantTechDataUpdate(it, emptyList()) }),
            supplierId, UUID.randomUUID(), "supplier",
        )

        result.updated.shouldBeEmpty()
        result.failed.map { it.productId } shouldBe listOf(failedId, unknownId)
        result.failed.first().message shouldBe "Kunne ikke lagre endringen"
        result.failed.last().message shouldBe "Varianten finnes ikke i denne serien"
        coVerify(exactly = 0) { repository.update(any<ProductRegistration>()) }
        coVerify(exactly = 0) { eventHandler.queueDTORapidEvent(any(), any(), any()) }
    }
}
