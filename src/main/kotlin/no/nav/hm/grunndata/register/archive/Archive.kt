package no.nav.hm.grunndata.register.archive

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity

import java.time.LocalDateTime
import java.util.UUID

@MappedEntity
data class Archive(
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val oid: UUID,
    val keywords: String? = null,
    val payload: String,
    val type: ArchiveType,
    val created: LocalDateTime = LocalDateTime.now(),
    val archivedByUser: String,
)

enum class ArchiveType {
    PRODUCT,
    PRODUCTAGREEMENT,
    SERIES,
 }