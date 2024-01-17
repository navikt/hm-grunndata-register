package no.nav.hm.grunndata.register.event

interface EventHandler  {

    fun isRapidEventType(eventItemType: EventItemType): Boolean
    fun sendRapidEvent(eventItem: EventItem)
    suspend fun queueDTORapidEvent(payload: EventPayload,
                                   eventName: String,
                                   extraKeyValues: Map<String, Any> = emptyMap())

}