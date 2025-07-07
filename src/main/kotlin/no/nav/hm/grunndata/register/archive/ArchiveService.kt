package no.nav.hm.grunndata.register.archive

import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
open class ArchiveService(private val archiveRepository: ArchiveRepository) {

    companion object {
        private val LOG = LoggerFactory.getLogger(ArchiveService::class.java)
    }

    private val archiveHandlers = mutableMapOf<Class<*>, ArchiveHandler<*>>()

    fun addArchiveHandler(handler: ArchiveHandler<*>) {
        val clazz = handler.getArchivePayloadClass()
        archiveHandlers[clazz] = handler
        LOG.info("Registered ArchiveHandler for payload class: ${clazz.simpleName}")
    }

    fun getArchiveHandler(clazz: Class<*>): ArchiveHandler<*>? = archiveHandlers[clazz]

    fun getAllHandlers(): Collection<ArchiveHandler<*>> = archiveHandlers.values

    suspend fun archiveAll() {
        LOG.info("Starting archiving process for all handlers")
        archiveHandlers.values.forEach { handler -> handleArchive(handler) }
    }

    @Transactional
    open suspend fun handleArchive(handler: ArchiveHandler<*>) {
        try {
            val archives = handler.archive()
            if (archives.isNotEmpty()) {
                archiveRepository.saveAll(archives)
                LOG.info("Archived ${archives.size} items for handler: ${handler::class.simpleName}")
            } else {
                LOG.info("No items to archive for handler: ${handler::class.simpleName}")
            }
        } catch (e: Exception) {
            LOG.error("Error during archiving with handler: ${handler::class.simpleName}", e)
        }
    }
}

