package no.nav.hm.grunndata.register.event

import no.nav.hm.grunndata.rapid.dto.RapidDTO

interface EventHandler  {

    fun getEventType(): EventItemType

    fun sendRapidEvent(eventItem: EventItem): RapidDTO

    fun getEventPayloadClass(): Class<out EventPayload>

    suspend fun queueDTORapidEvent(payload: EventPayload,
                                   eventName: String,
                                   extraKeyValues: Map<String, Any> = emptyMap())

}