package no.nav.hm.grunndata.register.bestillingsordning

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.event.*

@Singleton
class BestillingsordningEventHandler(
    private val registerRapidPushService: RegisterRapidPushService,
    private val objectMapper: ObjectMapper,
    private val eventItemService: EventItemService
) : DefaultEventHandler(eventItemService, objectMapper, registerRapidPushService) {

    override fun isRapidEventType(eventItemType: EventItemType): Boolean = eventItemType == EventItemType.BESTILLINGSORDNING

    override fun getEventType(): EventItemType = EventItemType.BESTILLINGSORDNING
    override fun getEventPayloadClass(): Class<out EventPayload> = BestillingsordningRegistrationDTO::class.java

}