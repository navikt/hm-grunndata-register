package no.nav.hm.grunndata.register.news

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.event.*

@Singleton
class NewsRegistrationEventHandler(registerRapidPushService: RegisterRapidPushService,
                                   objectMapper: ObjectMapper,
                                   eventItemService: EventItemService
): DefaultEventHandler(eventItemService, objectMapper, registerRapidPushService ) {

        override fun getEventType(): EventItemType = EventItemType.NEWS

    override fun isRapidEventType(eventItemType: EventItemType): Boolean  = eventItemType == EventItemType.NEWS

    override fun getEventPayloadClass(): Class<out EventPayload>  = NewsRegistrationDTO::class.java

}