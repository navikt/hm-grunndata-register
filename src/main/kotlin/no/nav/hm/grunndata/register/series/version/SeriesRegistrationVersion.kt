package no.nav.hm.grunndata.register.series.version

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.time.LocalDateTime
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.series.SeriesRegistration

@MappedEntity("series_reg_version_v1")
data class SeriesRegistrationVersion(
    @field:Id
    val versionId: UUID = UUID.randomUUID(),
    val seriesId: UUID,
    val status: SeriesStatus,
    val adminStatus: AdminStatus,
    val draftStatus: DraftStatus,
    val updated: LocalDateTime = LocalDateTime.now(),
    @field:TypeDef(type = DataType.JSON)
    val seriesRegistration: SeriesRegistration,
    val updatedBy: String,
    val version: Long? = 0L
)

data class SeriesRegistrationVersionDTO(
    val versionId: UUID,
    val seriesId: UUID,
    val status: SeriesStatus,
    val adminStatus: AdminStatus,
    val draftStatus: DraftStatus,
    val updated: LocalDateTime,
    val seriesRegistration: SeriesRegistration,
    val updatedBy: String,
    val version: Long
)

fun SeriesRegistrationVersion.toDTO(): SeriesRegistrationVersionDTO = SeriesRegistrationVersionDTO(
    versionId = this.versionId,
    seriesId = this.seriesId,
    status = this.status,
    adminStatus = this.adminStatus,
    draftStatus = this.draftStatus,
    updated = this.updated,
    seriesRegistration = this.seriesRegistration,
    updatedBy = this.updatedBy,
    version = this.version!!
)



