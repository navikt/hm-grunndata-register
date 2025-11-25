package no.nav.hm.grunndata.register.servicetask

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

@MicronautTest
class ServiceTaskRepositoryTest(private val serviceTaskRepository: ServiceTaskRepository) {

    @Test
    fun crudTest() = runBlocking {
        val now = LocalDateTime.now()
        val serviceTask = ServiceTask(
            id = UUID.randomUUID(),
            title = "Test Task",
            supplierRef = "SUP-123",
            hmsArtNr = "33445566",
            supplierId = UUID.randomUUID(),
            isoCategory = "112233",
            published = now,
            expired = now.plusYears(1),
            serviceData = ServiceData(
                attributes = ServiceTaskAttributes(
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
        val saved = serviceTaskRepository.save(serviceTask)
        assertNotNull(saved)
        assertEquals("Test Task", saved.title)
        assertEquals("33445566", saved.hmsArtNr)
        assertNotNull(saved.serviceData.attributes.serviceFor)

        // Read
        val found = serviceTaskRepository.findById(saved.id)
        assertNotNull(found)
        assertEquals("Test Description", found?.serviceData?.attributes?.text)

        // Update
        val updated = saved.copy(title = "Updated Task")
        val savedUpdated = serviceTaskRepository.update(updated)
        assertEquals("Updated Task", savedUpdated.title)

        // Delete
        serviceTaskRepository.deleteById(saved.id)
        val afterDelete = serviceTaskRepository.findById(saved.id)
        assertNull(afterDelete)
    }
}