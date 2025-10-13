package no.nav.hm.grunndata.register.part.hidden

import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Id
import java.time.LocalDateTime
import java.util.UUID

@MappedEntity("hidden_part_v1")
data class HiddenPart(
    @field:Id
    val productId: UUID,
    val reason: String? = null,
    val created: LocalDateTime = LocalDateTime.now(),
    val createdBy: String,
)

