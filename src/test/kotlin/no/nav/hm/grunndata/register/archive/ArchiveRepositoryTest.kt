package no.nav.hm.grunndata.register.archive

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest

import kotlinx.coroutines.runBlocking

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

@MicronautTest
class ArchiveRepositoryTest(private val archiveRepository: ArchiveRepository, private val objectMapper: ObjectMapper) {
    
    @Test
    fun crudTest() {
        runBlocking {
            val archive = Archive(
                id = UUID.randomUUID(),
                oid = UUID.randomUUID(),
                payload = objectMapper.writeValueAsString("key" to "value"),
                type = ArchiveType.PRODUCT,
                keywords = "test-keyword",
                created = LocalDateTime.now(),
                archivedByUser = "test-user"
            )
            archiveRepository.save(archive)
            val found = archiveRepository.findById(archive.id)
            found.shouldNotBeNull()
            found.type shouldBe ArchiveType.PRODUCT
            found.keywords shouldBe "test-keyword"
            found.disposeTime.shouldNotBeNull()
            found.disposeTime shouldBeAfter LocalDateTime.now()
        }
    }

}