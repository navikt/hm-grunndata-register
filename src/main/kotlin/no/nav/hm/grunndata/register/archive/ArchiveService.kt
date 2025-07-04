package no.nav.hm.grunndata.register.archive

import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
open class ArchiveService {

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
}