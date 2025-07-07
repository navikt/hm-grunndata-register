package no.nav.hm.grunndata.register.archive

import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Test

@MicronautTest
class ArchiveServiceTest(private val archiveService: ArchiveService
) {

    @Test
    fun archiveServiceTest() {
        runBlocking {
            archiveService.getAllHandlers().size shouldBe 1
            archiveService.archiveAll()
        }
    }
}