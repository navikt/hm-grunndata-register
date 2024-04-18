package no.nav.hm.grunndata.register.series

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.annotation.Version
import io.micronaut.data.model.DataType
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RapidDTO
import no.nav.hm.grunndata.rapid.dto.SeriesData
import no.nav.hm.grunndata.rapid.dto.SeriesRegistrationRapidDTO
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.event.EventPayload
import no.nav.hm.grunndata.register.product.MediaInfoDTO
import no.nav.hm.grunndata.register.product.toRapidMediaInfo
import java.time.LocalDateTime
import java.util.*

@MappedEntity("series_reg_v1")
data class SeriesRegistration(
    @field:Id
    val id: UUID,
    val supplierId: UUID,
    val identifier: String,
    val title: String,
    val text: String,
    val isoCategory: String,
    @field:TypeDef(type = DataType.JSON)
    val seriesData: SeriesDataDTO,
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val status: SeriesStatus = SeriesStatus.INACTIVE,
    val adminStatus: AdminStatus = AdminStatus.PENDING,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val expired: LocalDateTime = LocalDateTime.now().plusYears(15),
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    val updatedByUser: String = "system",
    val createdByUser: String = "system",
    val createdByAdmin: Boolean = false,
    @field:GeneratedValue
    val count: Int = 0,
    @field:Version
    val version: Long? = 0L
)

data class SeriesDataDTO(
    val media: Set<MediaInfoDTO> = emptySet()
)

data class SeriesRegistrationDTO(
    override val id: UUID,
    val supplierId: UUID,
    val identifier: String,
    val title: String,
    val text: String,
    val isoCategory: String,
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val adminStatus: AdminStatus = AdminStatus.PENDING,
    val status: SeriesStatus = SeriesStatus.ACTIVE,
    val seriesData: SeriesDataDTO,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val expired: LocalDateTime = LocalDateTime.now().plusYears(15),
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    override val updatedByUser: String = "system",
    val createdByUser: String = "system",
    val createdByAdmin: Boolean = false,
    val count: Int = 0,
    val version: Long? = 0L
) : EventPayload {
    override fun toRapidDTO(): RapidDTO = SeriesRegistrationRapidDTO(
        id = id,
        supplierId = supplierId,
        identifier = identifier,
        title = title,
        text = text,
        isoCategory = isoCategory,
        draftStatus = draftStatus,
        status = status,
        created = created,
        updated = updated,
        expired = expired,
        createdBy = createdBy,
        updatedBy = updatedBy,
        updatedByUser = updatedByUser,
        createdByUser = createdByUser,
        createdByAdmin = createdByAdmin,
        seriesData = seriesData.toRapidDTO(),
        version = version
    )
}

fun SeriesRegistration.toDTO() = SeriesRegistrationDTO(
    id = id,
    supplierId = supplierId,
    identifier = identifier,
    title = title,
    text = text,
    isoCategory = isoCategory,
    draftStatus = draftStatus,
    status = status,
    created = created,
    updated = updated,
    createdBy = createdBy,
    updatedBy = updatedBy,
    updatedByUser = updatedByUser,
    createdByUser = createdByUser,
    createdByAdmin = createdByAdmin,
    seriesData = seriesData,
    version = version,
    count = count
)

fun SeriesRegistrationDTO.toEntity() = SeriesRegistration(
    id = id, supplierId = supplierId, identifier = identifier, title = title, text = text, isoCategory = isoCategory,
    draftStatus = draftStatus,
    status = status, created = created, updated = updated, createdBy = createdBy, seriesData = seriesData,
    updatedBy = updatedBy, updatedByUser = updatedByUser, version = version, count = count
)

fun SeriesDataDTO.toRapidDTO() = SeriesData(media = media.map { it.toRapidMediaInfo() }.toSet())