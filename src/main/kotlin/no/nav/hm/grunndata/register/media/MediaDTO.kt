package no.nav.hm.grunndata.register.media

import no.nav.hm.grunndata.rapid.dto.MediaSourceType
import no.nav.hm.grunndata.rapid.dto.MediaType
import java.time.LocalDateTime
import java.util.*

data class MediaDTO(
    val id: UUID,
    val oid: UUID,
    val uri: String,
    val sourceUri: String,
    val filename: String?=null,
    val type: MediaType,
    val size: Long,
    val md5: String,
    val status: String,
    val source: MediaSourceType,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
)
