package no.nav.hm.grunndata.register.supplier

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.event.DefaultEventHandler
import no.nav.hm.grunndata.register.event.EventItemService
import no.nav.hm.grunndata.register.event.EventItemType
import no.nav.hm.grunndata.register.event.EventPayload
import no.nav.hm.grunndata.register.event.RegisterRapidPushService


@Singleton
class SupplierRegistrationHandler(private val registerRapidPushService: RegisterRapidPushService,
                                  private val objectMapper: ObjectMapper,
                                  private val eventItemService: EventItemService
): DefaultEventHandler(eventItemService, objectMapper, registerRapidPushService) {
    override fun getEventType(): EventItemType = EventItemType.SUPPLIER

    override fun getEventPayloadClass(): Class<out EventPayload> = SupplierRegistrationDTO::class.java

}
