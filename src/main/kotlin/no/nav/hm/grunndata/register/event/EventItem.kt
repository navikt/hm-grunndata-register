package no.nav.hm.grunndata.register.event

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.time.LocalDateTime
import java.util.*

@MappedEntity("event_item_v1")
data class EventItem(
    @field:Id
    val eventId: UUID = UUID.randomUUID(),
    val oid: UUID,
    val type: EventItemType,
    val status: EventItemStatus = EventItemStatus.PENDING,
    val eventName: String,
    val byUser: String,
    @field:TypeDef(type = DataType.JSON)
    val extraKeyValues: Map<String, Any> = emptyMap(),
    val payload: String,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now()
)

enum class EventItemStatus {
    PENDING,
    SENT
}

enum class EventItemType {
    AGREEMENT,
    PRODUCT,
    SERIES,
    SUPPLIER,
    PRODUCTAGREEMENT,
    BESTILLINGSORDNING
}

