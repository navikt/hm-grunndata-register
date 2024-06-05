package no.nav.hm.grunndata.register.series

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.event.DefaultEventHandler
import no.nav.hm.grunndata.register.event.EventItemService
import no.nav.hm.grunndata.register.event.EventItemType
import no.nav.hm.grunndata.register.event.EventPayload
import no.nav.hm.grunndata.register.event.RegisterRapidPushService

@Singleton
class SeriesRegistrationEventHandler(
    private val registerRapidPushService: RegisterRapidPushService,
    private val eventItemService: EventItemService,
    private val objectMapper: ObjectMapper
) : DefaultEventHandler(eventItemService, objectMapper, registerRapidPushService) {
    override fun getEventType(): EventItemType = EventItemType.SERIES

    override fun getEventPayloadClass(): Class<out EventPayload> = SeriesRegistrationDTO::class.java

}

