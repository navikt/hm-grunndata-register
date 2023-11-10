package no.nav.hm.grunndata.register.series

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Version
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.event.EventPayload
import java.time.LocalDateTime
import java.util.*

@MappedEntity("series_reg_v1")
data class SeriesRegistration (
    @field:Id
    val id: UUID,
    val supplierId:UUID,
    val identifier: String,
    val title: String,
    val text: String,
    val isoCategory: String,
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val status: SeriesStatus = SeriesStatus.INACTIVE,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val expired: LocalDateTime = LocalDateTime.now().plusYears(15),
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    val updatedByUser: String = "system",
    val createdByUser: String = "system",
    val createdByAdmin: Boolean = false,
    @field:Version
    val version: Long? = 0L
)

data class SeriesRegistrationDTO (
    val id: UUID,
    val supplierId:UUID,
    val identifier: String,
    val title: String,
    val text: String,
    val isoCategory: String,
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val status: SeriesStatus = SeriesStatus.ACTIVE,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val expired: LocalDateTime = LocalDateTime.now().plusYears(15),
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    val updatedByUser: String = "system",
    val createdByUser: String = "system",
    val createdByAdmin: Boolean = false,
    val version: Long? = 0L
): EventPayload

fun SeriesRegistration.toDTO() = SeriesRegistrationDTO(id = id, supplierId = supplierId, identifier = identifier,
    title = title, text = text, isoCategory = isoCategory, draftStatus = draftStatus, status = status, created = created,
    updated = updated, createdBy = createdBy, updatedBy=updatedBy, updatedByUser = updatedByUser, createdByUser = createdByUser,
    createdByAdmin = createdByAdmin, version = version
)

fun SeriesRegistrationDTO.toEntity() = SeriesRegistration(
    id = id, supplierId = supplierId, identifier = identifier, title = title, text=text, isoCategory=isoCategory,
    draftStatus = draftStatus,
    status = status, created = created, updated = updated, createdBy = createdBy,
    updatedBy = updatedBy, updatedByUser = updatedByUser, version = version
)
