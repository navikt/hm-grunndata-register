package no.nav.hm.grunndata.register.servicejob

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.annotation.Version
import io.micronaut.data.model.DataType
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.ServiceAgreementInfo
import no.nav.hm.grunndata.rapid.dto.ServiceAttributes
import no.nav.hm.grunndata.rapid.dto.ServiceJobRapidDTO
import no.nav.hm.grunndata.rapid.dto.ServiceStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.event.EventPayload
import java.time.LocalDateTime
import java.util.UUID

@MappedEntity("service_job_v1")
data class ServiceJob(
    @field:Id
    val id: UUID,
    val title: String,
    val supplierRef: String?,
    val hmsArtNr: String,
    val supplierId: UUID,
    val isoCategory: String,
    val published: LocalDateTime,
    val expired: LocalDateTime,
    val updated: LocalDateTime = LocalDateTime.now(),
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val status: ServiceStatus = ServiceStatus.ACTIVE,
    val created: LocalDateTime = LocalDateTime.now(),
    val updatedBy: String = REGISTER,
    val createdBy: String = REGISTER,
    val createdByUser: String = "system",
    val updatedByUser: String = "system",
    @field:TypeDef(type = DataType.JSON)
    val attributes: ServiceAttributes = ServiceAttributes(),
    @field:Version
    val version: Long? = 0L,
)


data class ServiceJobDTO(
    override val id: UUID,
    val title: String,
    val supplierId: UUID,
    val supplierRef: String?,
    val hmsNr: String,
    val isoCategory: String,
    val published: LocalDateTime,
    val expired: LocalDateTime,
    val updated: LocalDateTime = LocalDateTime.now(),
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val status: ServiceStatus = ServiceStatus.ACTIVE,
    val created: LocalDateTime = LocalDateTime.now(),
    val updatedBy: String = REGISTER,
    val createdBy: String = REGISTER,
    val createdByUser: String = "system",
    override val updatedByUser: String = "system",
    val attributes: ServiceAttributes,
    val agreements: List<ServiceAgreementInfo> = emptyList(),
    val version: Long? = 0L,
): EventPayload {
    override fun toRapidDTO(): ServiceJobRapidDTO = ServiceJobRapidDTO(
        id = id,
        title = title,
        supplierId = supplierId,
        supplierRef = supplierRef,
        hmsNr = hmsNr,
        isoCategory = isoCategory,
        published = published,
        expired = expired,
        updated = updated,
        draftStatus = draftStatus,
        status = status,
        created = created,
        updatedBy = updatedBy,
        createdBy = createdBy,
        createdByUser = createdByUser,
        updatedByUser = updatedByUser,
        agreements = agreements,
        attributes = attributes,
    )
}

