package no.nav.hm.grunndata.register.servicejob

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.ServiceAttributes
import no.nav.hm.grunndata.rapid.dto.ServiceFor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

@MicronautTest
class ServiceJobRepositoryTest(private val serviceJobRepository: ServiceJobRepository) {

    @Test
    fun crudTest() = runBlocking {
        val now = LocalDateTime.now()
        val serviceJob = ServiceJob(
            id = UUID.randomUUID(),
            title = "Test Task",
            supplierRef = "SUP-123",
            hmsArtNr = "33445566",
            supplierId = UUID.randomUUID(),
            isoCategory = "112233",
            published = now,
            expired = now.plusYears(1),
            attributes = ServiceAttributes(
                keywords = setOf("keyword1", "keyword2"),
                serviceFor = ServiceFor(
                    seriesIds = setOf(UUID.randomUUID()),
                    productIds = setOf(UUID.randomUUID())
                ),
                url = "http://example.com",
                text = "Test Description"
            )
        )

        // Create
        val saved = serviceJobRepository.save(serviceJob)
        assertNotNull(saved)
        assertEquals("Test Task", saved.title)
        assertEquals("33445566", saved.hmsArtNr)
        assertNotNull(saved.attributes.serviceFor)

        // Read
        val found = serviceJobRepository.findById(saved.id)
        assertNotNull(found)
        assertEquals("Test Description", found?.attributes?.text)

        // Update
        val updated = saved.copy(title = "Updated Task")
        val savedUpdated = serviceJobRepository.update(updated)
        assertEquals("Updated Task", savedUpdated.title)

        // Delete
        serviceJobRepository.deleteById(saved.id)
        val afterDelete = serviceJobRepository.findById(saved.id)
        assertNull(afterDelete)
    }
}