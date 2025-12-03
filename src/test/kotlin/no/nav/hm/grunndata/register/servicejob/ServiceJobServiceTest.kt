package no.nav.hm.grunndata.register.servicejob

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime
import java.util.*

@MicronautTest
class ServiceJobServiceTest(private val serviceJobService: ServiceJobService) {


    @Test
    fun `should save and create event if not draft`() = runBlocking {
        val job = ServiceJob(
            id = UUID.randomUUID(),
            supplierId = UUID.randomUUID(),
            supplierRef = "SUP-REF",
            hmsArtNr = "HMS-123",
            title = "Test Service Job",
            published = LocalDateTime.now(),
            expired = LocalDateTime.now().plusYears(1),
            created = LocalDateTime.now(),
            updated = LocalDateTime.now(),
            draftStatus = DraftStatus.DONE,
            isoCategory = "123456"
        )
        val saved = serviceJobService.saveAndCreateEventIfNotDraft(job)
        assertEquals(job.id, saved.id)
        assertEquals(saved.draftStatus, DraftStatus.DONE)

    }

    @Test
    fun `should not create event if draft status is not DONE`() = runBlocking {
        val job = ServiceJob(
            id = UUID.randomUUID(),
            supplierId = UUID.randomUUID(),
            supplierRef = "SUP-REF",
            hmsArtNr = "HMS-123",
            title = "Test Service Job",
            published = LocalDateTime.now(),
            expired = LocalDateTime.now().plusYears(1),
            created = LocalDateTime.now(),
            updated = LocalDateTime.now(),
            draftStatus = DraftStatus.DRAFT,
            isoCategory = "123456"
        )

        val saved = serviceJobService.saveAndCreateEventIfNotDraft(job)
        assertEquals(job.id, saved.id)
        assertEquals(saved.draftStatus, DraftStatus.DRAFT)

    }
}