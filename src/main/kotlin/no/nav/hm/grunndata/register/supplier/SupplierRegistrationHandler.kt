package no.nav.hm.grunndata.register.supplier

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.event.*


@Singleton
class SupplierRegistrationHandler(private val registerRapidPushService: RegisterRapidPushService,
                                  private val objectMapper: ObjectMapper,
                                  private val eventItemService: EventItemService
): DefaultEventHandler(eventItemService, objectMapper, registerRapidPushService) {
    override fun getEventType(): EventItemType = EventItemType.SUPPLIER

    override fun isRapidEventType(eventItemType: EventItemType): Boolean  = eventItemType == EventItemType.SUPPLIER

    override fun getEventPayloadClass(): Class<out EventPayload> = SupplierRegistrationDTO::class.java

}
