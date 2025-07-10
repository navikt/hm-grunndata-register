package no.nav.hm.grunndata.register.archive

import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.micronaut.leaderelection.LeaderOnly

@Singleton
open class ArchiveScheduler(private val archiveService: ArchiveService) {

    @LeaderOnly
    @Scheduled(cron = "30 * * * * *")
    open fun archiveAll() {
        runBlocking {
            archiveService.archiveAll()
            archiveService.unarchiveAll()
        }
    }

}