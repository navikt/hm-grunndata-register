package no.nav.hm.grunndata.register.news

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.event.DefaultEventHandler
import no.nav.hm.grunndata.register.event.EventItemService
import no.nav.hm.grunndata.register.event.EventItemType
import no.nav.hm.grunndata.register.event.EventPayload
import no.nav.hm.grunndata.register.event.RegisterRapidPushService

@Singleton
class NewsRegistrationEventHandler(registerRapidPushService: RegisterRapidPushService,
                                   objectMapper: ObjectMapper,
                                   eventItemService: EventItemService
): DefaultEventHandler(eventItemService, objectMapper, registerRapidPushService ) {

        override fun getEventType(): EventItemType = EventItemType.NEWS


    override fun getEventPayloadClass(): Class<out EventPayload>  = NewsRegistrationDTO::class.java

}