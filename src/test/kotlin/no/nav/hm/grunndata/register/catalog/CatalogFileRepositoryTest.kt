package no.nav.hm.grunndata.register.catalog

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.data.model.Pageable
import io.micronaut.http.MediaType
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import java.io.InputStream
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

@MicronautTest
class CatalogFileRepositoryTest(private val catalogFileRepository: CatalogFileRepository) {

    @Test
    fun testRepository() {
        val resourceStream = CatalogFileRepositoryTest::class.java.getResourceAsStream("/productagreement/katalog-test.xls")
        val completedFileUpload = CustomCompletedFileUpload(
            inputStream = resourceStream,
            filename = "katalog-test.xls",
            size = resourceStream.available().toLong()
        )
        val testCatalogFile = CatalogFile(
            name = "katalog-test.xls",
            size = completedFileUpload.size,
            file = completedFileUpload.bytes,
            supplierId = UUID.randomUUID(),
            createdBy = "test",
            created = LocalDateTime.now(),
            updated = LocalDateTime.now(),
            status = CatalogFileStatus.PENDING
        )
        runBlocking {
            val saved = catalogFileRepository.save(testCatalogFile)
            saved.shouldNotBeNull()
            val id = saved.id
            val found = catalogFileRepository.findById(id)
            val foundDTO = catalogFileRepository.findOne(id)
            found.shouldNotBeNull()
            foundDTO.shouldNotBeNull()
            found.size shouldBe foundDTO.size
            found.name shouldBe foundDTO.name
            catalogFileRepository.findMany(Pageable.from(0, 10)).content.size shouldBe 1
        }
    }
}

class CustomCompletedFileUpload(
    private val inputStream: InputStream,
    private val filename: String,
    private val size: Long
) : CompletedFileUpload {

    override fun getContentType(): Optional<MediaType> {
        TODO("Not yet implemented")
    }

    override fun getName(): String = filename
    override fun getFilename(): String = filename
    override fun getSize(): Long = size

    override fun getDefinedSize(): Long = size

    override fun isComplete(): Boolean {
        return true
    }

    override fun getBytes(): ByteArray = inputStream.readBytes()
    override fun getByteBuffer(): ByteBuffer {
        return ByteBuffer.wrap(inputStream.readBytes())
    }
    override fun getInputStream(): InputStream = inputStream
}