package no.nav.hm.grunndata.register.media

import java.time.LocalDateTime
import java.util.*

data class MediaDTO(
    val id: UUID,
    val oid: UUID,
    val uri: String,
    val sourceUri: String,
    val type: String,
    val size: Long,
    val md5: String,
    val status: String,
    val source: String,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
)
