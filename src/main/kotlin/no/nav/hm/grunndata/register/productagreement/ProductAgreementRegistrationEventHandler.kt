package no.nav.hm.grunndata.register.productagreement

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.event.*

@Singleton
class ProductAgreementRegistrationEventHandler(private val registerRapidPushService: RegisterRapidPushService,
                                               private val objectMapper: ObjectMapper,
                                               private val eventItemService: EventItemService):
    DefaultEventHandler(eventItemService, objectMapper, registerRapidPushService) {


    override fun getEventType(): EventItemType = EventItemType.PRODUCTAGREEMENT

    override fun isRapidEventType(eventItemType: EventItemType): Boolean = eventItemType == EventItemType.PRODUCTAGREEMENT

    override fun getEventPayloadClass(): Class<out EventPayload> = ProductAgreementRegistrationDTO::class.java

}