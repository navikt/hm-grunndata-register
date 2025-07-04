package no.nav.hm.grunndata.register.archive

import io.micronaut.context.annotation.Context
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import org.slf4j.LoggerFactory

@Context
class ArchiveHandlerRegister(
    private val archiveService: ArchiveService
) : BeanCreatedEventListener<ArchiveHandler<*>> {

    companion object {
        private val LOG = LoggerFactory.getLogger(ArchiveHandlerRegister::class.java)
    }

    override fun onCreated(event: BeanCreatedEvent<ArchiveHandler<*>>): ArchiveHandler<*> {
        LOG.info("Found ArchiveHandler for: ${event.bean.getArchivePayloadClass().simpleName}")
        archiveService.addArchiveHandler(event.bean)
        return event.bean
    }
}