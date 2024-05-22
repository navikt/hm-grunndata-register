package no.nav.hm.grunndata.register.event

import no.nav.hm.grunndata.rapid.dto.RapidDTO
import java.time.LocalDateTime
import java.util.*

interface EventPayload {
    val id: UUID
    val updatedByUser: String
    val publicationDate: LocalDateTime?
    fun toRapidDTO(): RapidDTO
}
