package no.nav.hm.grunndata.register.serviceoffering

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.annotation.Version
import io.micronaut.data.model.DataType
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.register.REGISTER
import java.time.LocalDateTime
import java.util.UUID

@MappedEntity("service_offering_v1")
data class ServiceOffering(
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
    val serviceData: ServiceData,
    @field:Version
    val version: Long? = 0L,
)

enum class ServiceStatus {
    ACTIVE, INACTIVE, DELETED
}

data class ServiceData (
    val attributes: ServiceAttributes = ServiceAttributes(),
)

data class ServiceAttributes(
    val keywords: Set<String>? = null,
    val url: String? = null,
    val text: String? = null,
    val serviceFor: ServiceFor? = null,
)

data class ServiceFor(
    val seriesIds: Set<UUID> = emptySet(), val productIds: Set<UUID> = emptySet()
)
