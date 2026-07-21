package no.nav.hm.grunndata.register.iso.v22

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.LocalDateTime
import java.util.UUID

@MappedEntity("iso_map_v22")
data class IsoMap(
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val code16: String?=null,
    val code22: String?=null,
    val created: LocalDateTime = LocalDateTime.now(),
)