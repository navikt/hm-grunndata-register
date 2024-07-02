package no.nav.hm.grunndata.register.product.attributes.produkttype

import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Id
import no.nav.hm.grunndata.rapid.dto.ProdukttypeRegistrationRapidDTO
import no.nav.hm.grunndata.rapid.dto.ProdukttypeStatus
import no.nav.hm.grunndata.rapid.dto.RapidDTO
import no.nav.hm.grunndata.register.event.EventPayload
import java.time.LocalDateTime
import java.util.*

@MappedEntity("produkttype_reg_v1")
data class ProdukttypeRegistration(
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val isokode: String,
    val produkttype: String,
    val status: ProdukttypeStatus = ProdukttypeStatus.ACTIVE,
    val updatedByUser: String = "system",
    val createdByUser: String = "system",
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val deactivated: LocalDateTime? = null
)

data class ProdukttypeRegistrationDTO(
    override val id: UUID = UUID.randomUUID(),
    val isokode: String,
    val produkttype: String,
    val status: ProdukttypeStatus = ProdukttypeStatus.ACTIVE,
    override val updatedByUser: String = "system",
    val createdByUser: String = "system",
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val deactivated: LocalDateTime? = null
): EventPayload {
    override fun toRapidDTO(): RapidDTO = ProdukttypeRegistrationRapidDTO(
        id = id, isokode = isokode, produkttype = produkttype, status = status,
        updatedByUser = updatedByUser, createdByUser = createdByUser, created = created, updated = updated,
        deactivated = deactivated
    )
}

fun ProdukttypeRegistration.toDTO(): ProdukttypeRegistrationDTO = ProdukttypeRegistrationDTO(
    id = id,
    isokode = isokode,
    produkttype = produkttype,
    status = status,
    updatedByUser = updatedByUser,
    createdByUser = createdByUser,
    created = created,
    updated = updated,
    deactivated = deactivated,
)

fun ProdukttypeRegistrationDTO.toEntity(): ProdukttypeRegistration = ProdukttypeRegistration(
    id = id,
    isokode = isokode,
    produkttype = produkttype,
    status = status,
    updatedByUser = updatedByUser,
    createdByUser = createdByUser,
    created = created,
    updated = updated,
    deactivated = deactivated,
)
