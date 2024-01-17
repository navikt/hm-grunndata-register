package no.nav.hm.grunndata.register.event

interface EventHandler  {

    fun getEventType(): EventItemType

    fun isRapidEventType(eventItemType: EventItemType): Boolean
    fun sendRapidEvent(eventItem: EventItem)

    fun getEventPayloadClass(): Class<out EventPayload>

    suspend fun queueDTORapidEvent(payload: EventPayload,
                                   eventName: String,
                                   extraKeyValues: Map<String, Any> = emptyMap())

}