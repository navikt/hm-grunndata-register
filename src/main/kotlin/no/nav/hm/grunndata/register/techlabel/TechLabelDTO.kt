package no.nav.hm.grunndata.register.techlabel

import java.time.LocalDateTime
import java.util.*

data class TechLabelDTO(
    val id: UUID,
    val identifier: String,
    val label: String,
    val guide: String,
    val isocode: String,
    val type: String,
    val unit: String?,
    val createdBy: String,
    val updatedBy: String,
    val created: LocalDateTime,
    val updated: LocalDateTime
)