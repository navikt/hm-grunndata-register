package no.nav.hm.grunndata.register.serviceoffering

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

@MicronautTest
class ServiceOfferingRepositoryTest(private val serviceOfferingRepository: ServiceOfferingRepository) {

    @Test
    fun crudTest() = runBlocking {
        val now = LocalDateTime.now()
        val serviceOffering = ServiceOffering(
            id = UUID.randomUUID(),
            title = "Test Task",
            supplierRef = "SUP-123",
            hmsArtNr = "33445566",
            supplierId = UUID.randomUUID(),
            isoCategory = "112233",
            published = now,
            expired = now.plusYears(1),
            serviceData = ServiceData(
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
        )

        // Create
        val saved = serviceOfferingRepository.save(serviceOffering)
        assertNotNull(saved)
        assertEquals("Test Task", saved.title)
        assertEquals("33445566", saved.hmsArtNr)
        assertNotNull(saved.serviceData.attributes.serviceFor)

        // Read
        val found = serviceOfferingRepository.findById(saved.id)
        assertNotNull(found)
        assertEquals("Test Description", found?.serviceData?.attributes?.text)

        // Update
        val updated = saved.copy(title = "Updated Task")
        val savedUpdated = serviceOfferingRepository.update(updated)
        assertEquals("Updated Task", savedUpdated.title)

        // Delete
        serviceOfferingRepository.deleteById(saved.id)
        val afterDelete = serviceOfferingRepository.findById(saved.id)
        assertNull(afterDelete)
    }
}