package no.nav.hm.grunndata.register.event

import io.micronaut.context.annotation.Context
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener

@Context
class EventHandlerRegister(private val eventItemService: EventItemService): BeanCreatedEventListener<EventHandler> {

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(EventHandlerRegister::class.java)
    }

    override fun onCreated(event: BeanCreatedEvent<EventHandler>): EventHandler {
        LOG.info("Found Eventhandler for : ${event.bean.getEventType()}")
        eventItemService.addEventHandler(event.bean)
        return event.bean
    }

}