package no.nav.hm.grunndata.register.product.attributes.produkttype

import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Id
import no.nav.hm.grunndata.rapid.dto.ProduktTypeRegistrationRapidDTO
import no.nav.hm.grunndata.rapid.dto.ProduktTypeStatus
import no.nav.hm.grunndata.rapid.dto.RapidDTO
import no.nav.hm.grunndata.register.event.EventPayload
import java.time.LocalDateTime
import java.util.*

@MappedEntity("produkttype_reg_v1")
data class ProduktTypeRegistration(
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val sortimentKategori: String,
    val postId: UUID,
    val status: ProduktTypeStatus = ProduktTypeStatus.ACTIVE,
    val updatedByUser: String = "system",
    val createdByUser: String = "system",
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val deactivated: LocalDateTime? = null
)

data class ProduktTypeRegistrationDTO(
    override val id: UUID = UUID.randomUUID(),
    val sortimentKategori: String,
    val postId: UUID,
    val status: ProduktTypeStatus = ProduktTypeStatus.ACTIVE,
    override val updatedByUser: String = "system",
    val createdByUser: String = "system",
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val deactivated: LocalDateTime? = null
): EventPayload {
    override fun toRapidDTO(): RapidDTO = ProduktTypeRegistrationRapidDTO(
        id = id, sortimentKategori = sortimentKategori, postId = postId, status = status,
        updatedByUser = updatedByUser, createdByUser = createdByUser, created = created, updated = updated,
        deactivated = deactivated
    )
}

fun ProduktTypeRegistration.toDTO(): ProduktTypeRegistrationDTO = ProduktTypeRegistrationDTO(
    id = id,
    sortimentKategori = sortimentKategori,
    postId = postId,
    status = status,
    updatedByUser = updatedByUser,
    createdByUser = createdByUser,
    created = created,
    updated = updated,
    deactivated = deactivated,
)

fun ProduktTypeRegistrationDTO.toEntity(): ProduktTypeRegistration = ProduktTypeRegistration(
    id = id,
    sortimentKategori = sortimentKategori,
    postId = postId,
    status = status,
    updatedByUser = updatedByUser,
    createdByUser = createdByUser,
    created = created,
    updated = updated,
    deactivated = deactivated,
)
