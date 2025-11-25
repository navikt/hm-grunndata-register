package no.nav.hm.grunndata.register.servicetask

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.annotation.Version
import io.micronaut.data.model.DataType
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import java.time.LocalDateTime
import java.util.UUID

@MappedEntity("service_task_v1")
data class ServiceTask(
    @field:Id
    val id: UUID,
    val title: String,
    val supplierRef: String?,
    val hmsArtNr: String,
    val supplierId: UUID,
    val isoCategory: String,
    val published: LocalDateTime,
    val expired: LocalDateTime,
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val status: ServiceTaskStatus = ServiceTaskStatus.ACTIVE,
    val created: LocalDateTime = LocalDateTime.now(),
    @field:TypeDef(type = DataType.JSON)
    val serviceData: ServiceData,
    @field:Version
    val version: Long? = 0L,
)

enum class ServiceTaskStatus {
    ACTIVE, INACTIVE, DELETED
}

data class ServiceData (
    val attributes: ServiceTaskAttributes = ServiceTaskAttributes(),
)

data class ServiceTaskAttributes(
    val keywords: Set<String>? = null,
    val url: String? = null,
    val text: String? = null,
    val serviceFor: ServiceFor? = null,
)

data class ServiceFor(
    val seriesIds: Set<UUID> = emptySet(), val productIds: Set<UUID> = emptySet()
)
