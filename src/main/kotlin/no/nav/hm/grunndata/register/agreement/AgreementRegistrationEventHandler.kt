package no.nav.hm.grunndata.register.agreement

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.event.*

@Singleton
class AgreementRegistrationEventHandler(private val registerRapidPushService: RegisterRapidPushService,
                                        private val objectMapper: ObjectMapper,
                                        private val eventItemService: EventItemService
): DefaultEventHandler(eventItemService, objectMapper, registerRapidPushService) {
    override fun getEventType(): EventItemType = EventItemType.AGREEMENT

    override fun isRapidEventType(eventItemType: EventItemType): Boolean  = eventItemType == EventItemType.AGREEMENT

    override fun getEventPayloadClass(): Class<out EventPayload> = AgreementRegistrationDTO::class.java


}