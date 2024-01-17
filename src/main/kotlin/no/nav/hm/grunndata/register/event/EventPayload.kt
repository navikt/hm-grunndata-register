package no.nav.hm.grunndata.register.event

import no.nav.hm.grunndata.rapid.dto.RapidDTO
import java.util.*

interface EventPayload {
    val id: UUID
    val updatedByUser: String

    fun toRapidDTO(): RapidDTO
}
