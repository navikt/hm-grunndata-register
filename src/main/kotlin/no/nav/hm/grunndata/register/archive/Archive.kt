package no.nav.hm.grunndata.register.archive

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity

import java.time.LocalDateTime
import java.util.UUID

@MappedEntity("archive_v1")
data class Archive(
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val oid: UUID,
    val keywords: String,
    val payload: String,
    val type: ArchiveType,
    val status: ArchiveStatus = ArchiveStatus.ARCHIVED,
    val created: LocalDateTime = LocalDateTime.now(),
    val disposeTime: LocalDateTime = LocalDateTime.now().plusDays(30),
    val archivedByUser: String,
)

enum class ArchiveType {
    PRODUCT,
    PRODUCTAGREEMENT
}

enum class ArchiveStatus {
    ARCHIVED,
    UNARCHIVE,
}
