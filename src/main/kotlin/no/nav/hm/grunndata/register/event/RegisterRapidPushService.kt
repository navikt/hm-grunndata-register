package no.nav.hm.grunndata.register.event

import jakarta.inject.Singleton
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.RapidDTO
import no.nav.hm.grunndata.rapid.dto.rapidDTOVersion
import no.nav.hm.grunndata.rapid.event.RapidApp
import no.nav.hm.rapids_rivers.micronaut.RapidPushService

@Singleton
class RegisterRapidPushService(private val kafkaRapidService: RapidPushService) {

    private fun pushDTOToKafka(dto: RapidDTO, eventName: String, extraKeyValues: Map<String, Any> = emptyMap(),
                               eventId: UUID = UUID.randomUUID()) {
        kafkaRapidService.pushToRapid(
            key = dto.partitionKey,
            eventName = eventName, payload = dto,
            keyValues = mapOf("createdBy" to RapidApp.grunndata_register,
                "dtoVersion" to rapidDTOVersion) + extraKeyValues,
            eventId = eventId
        )
    }

    fun pushToRapid(dto:RapidDTO, eventItem: EventItem) = pushDTOToKafka(dto, eventItem.eventName, eventItem.extraKeyValues,
        eventId = eventItem.eventId)


}
