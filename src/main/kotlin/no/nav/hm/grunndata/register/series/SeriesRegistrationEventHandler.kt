package no.nav.hm.grunndata.register.series

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.event.*

@Singleton
class SeriesRegistrationEventHandler(
    private val registerRapidPushService: RegisterRapidPushService,
    private val eventItemService: EventItemService,
    private val objectMapper: ObjectMapper
) : DefaultEventHandler(eventItemService, objectMapper, registerRapidPushService) {
    override fun getEventType(): EventItemType = EventItemType.SERIES

    override fun isRapidEventType(eventItemType: EventItemType): Boolean = eventItemType == EventItemType.SERIES

    override fun getEventPayloadClass(): Class<out EventPayload> = SeriesRegistrationDTO::class.java

}

