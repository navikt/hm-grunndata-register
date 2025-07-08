package no.nav.hm.grunndata.register.archive

import io.micronaut.transaction.TransactionDefinition
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.collect
import org.slf4j.LoggerFactory

@Singleton
open class ArchiveService(private val archiveRepository: ArchiveRepository) {

    companion object {
        private val LOG = LoggerFactory.getLogger(ArchiveService::class.java)
    }

    private val archiveHandlers = mutableMapOf<ArchiveType, ArchiveHandler>()

    fun addArchiveHandler(handler: ArchiveHandler) {
        val type = handler.getArchiveType()
        archiveHandlers[type] = handler
        LOG.info("Registered ArchiveHandler for type: ${type.name} with class: ${handler::class.simpleName}")
    }

    fun getAllHandlers(): Collection<ArchiveHandler> = archiveHandlers.values

    suspend fun archiveAll() {
        LOG.info("Starting archiving process for all ${archiveHandlers.size} handlers")
        for (handler in archiveHandlers.values) {
            try {
                handleArchive(handler)
            } catch (e: Exception) {
                LOG.warn("Error during archiving with handler: ${handler::class.simpleName} ${e.message}")
            }
        }
    }

    suspend fun unarchiveAll() {
        LOG.info("Starting unarchiving process for all ${archiveHandlers.size} handlers")
        for (handler in archiveHandlers.values) {
            try {
                handleUnarchive(handler)
            } catch (e: Exception) {
                LOG.error("Error during unarchiving with handler: ${handler::class.simpleName}", e)
            }
        }
    }

    @Transactional(propagation = TransactionDefinition.Propagation.REQUIRES_NEW)
    open suspend fun handleArchive(handler: ArchiveHandler) {

        val archives = handler.archive()
        if (archives.isNotEmpty()) {
            archiveRepository.saveAll(archives).collect()
            LOG.info("Archived ${archives.size} items for handler: ${handler::class.simpleName}")
        } else {
            LOG.info("No items to archive for handler: ${handler::class.simpleName}")
        }
    }

    @Transactional(propagation = TransactionDefinition.Propagation.REQUIRES_NEW)
    open suspend fun handleUnarchive(handler: ArchiveHandler) {
        try {
            LOG.info("Starting unarchiving process for handler: ${handler::class.simpleName}")
            val unarchives = archiveRepository.findByStatusAndType(
                ArchiveStatus.UNARCHIVE, handler.getArchiveType()
            )
            if (unarchives.isNotEmpty()) {
                unarchives.forEach { unarchive ->
                    handler.unArchive(unarchive)
                    archiveRepository.deleteById(unarchive.id)
                }
            }
        } catch (e: Exception) {
            LOG.error("Error during unarchiving with handler: ${handler::class.simpleName}", e)
        }
    }
}

