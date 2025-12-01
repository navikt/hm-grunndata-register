package no.nav.hm.grunndata.register.servicejob

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

@MicronautTest
class ServiceAgreementRepositoryTest(private val serviceAgreementRepository: ServiceAgreementRepository) {

    @Test
    fun crudTest() = runBlocking {
        val now = LocalDateTime.now()
        val agreement = ServiceAgreement(
            id = UUID.randomUUID(),
            serviceId = UUID.randomUUID(),
            supplierId = UUID.randomUUID(),
            supplierRef = "SUP-AGREE-1",
            agreementId = UUID.randomUUID(),
            published = now,
            expired = now.plusYears(1)
        )

        // Create
        val saved = serviceAgreementRepository.save(agreement)
        assertNotNull(saved)
        assertEquals("SUP-AGREE-1", saved.supplierRef)

        // Read
        val found = serviceAgreementRepository.findById(saved.id)
        assertNotNull(found)
        assertEquals(saved.id, found?.id)

        // Update
        val updated = saved.copy(supplierRef = "SUP-AGREE-UPDATED")
        val savedUpdated = serviceAgreementRepository.update(updated)
        assertEquals("SUP-AGREE-UPDATED", savedUpdated.supplierRef)

        // Delete
        serviceAgreementRepository.deleteById(saved.id)
        val afterDelete = serviceAgreementRepository.findById(saved.id)
        assertNull(afterDelete)
    }
}