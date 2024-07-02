package no.nav.hm.grunndata.register.product.attributes.digitalsoknad

import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Id
import no.nav.hm.grunndata.rapid.dto.DigitalSoknadSortimentRegistrationRapidDTO
import no.nav.hm.grunndata.rapid.dto.DigitalSoknadSortimentStatus
import no.nav.hm.grunndata.rapid.dto.RapidDTO
import no.nav.hm.grunndata.register.event.EventPayload
import java.time.LocalDateTime
import java.util.*

@MappedEntity("digitalsoknadsortiment_reg_v1")
data class DigitalSoknadSortimentRegistration(
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val sortimentKategori: String,
    val postId: UUID,
    val status: DigitalSoknadSortimentStatus = DigitalSoknadSortimentStatus.ACTIVE,
    val updatedByUser: String = "system",
    val createdByUser: String = "system",
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val deactivated: LocalDateTime? = null
)

data class DigitalSoknadSortimentRegistrationDTO(
    override val id: UUID = UUID.randomUUID(),
    val sortimentKategori: String,
    val postId: UUID,
    val status: DigitalSoknadSortimentStatus = DigitalSoknadSortimentStatus.ACTIVE,
    override val updatedByUser: String = "system",
    val createdByUser: String = "system",
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val deactivated: LocalDateTime? = null
): EventPayload {
    override fun toRapidDTO(): RapidDTO = DigitalSoknadSortimentRegistrationRapidDTO(
        id = id, sortimentKategori = sortimentKategori, postId = postId, status = status,
        updatedByUser = updatedByUser, createdByUser = createdByUser, created = created, updated = updated,
        deactivated = deactivated
    )
}

fun DigitalSoknadSortimentRegistration.toDTO(): DigitalSoknadSortimentRegistrationDTO = DigitalSoknadSortimentRegistrationDTO(
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

fun DigitalSoknadSortimentRegistrationDTO.toEntity(): DigitalSoknadSortimentRegistration = DigitalSoknadSortimentRegistration(
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
