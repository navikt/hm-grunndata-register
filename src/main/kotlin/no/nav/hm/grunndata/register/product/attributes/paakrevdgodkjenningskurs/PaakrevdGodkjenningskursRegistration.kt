package no.nav.hm.grunndata.register.product.attributes.paakrevdgodkjenningskurs

import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Id
import no.nav.hm.grunndata.rapid.dto.PaakrevdGodkjenningskursRegistrationRapidDTO
import no.nav.hm.grunndata.rapid.dto.PaakrevdGodkjenningskursStatus
import no.nav.hm.grunndata.rapid.dto.RapidDTO
import no.nav.hm.grunndata.register.event.EventPayload
import java.time.LocalDateTime
import java.util.*

@MappedEntity("paakrevdgodkjenningskurs_reg_v1")
data class PaakrevdGodkjenningskursRegistration(
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val sortimentKategori: String,
    val postId: UUID,
    val status: PaakrevdGodkjenningskursStatus = PaakrevdGodkjenningskursStatus.ACTIVE,
    val updatedByUser: String = "system",
    val createdByUser: String = "system",
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val deactivated: LocalDateTime? = null
)

data class PaakrevdGodkjenningskursRegistrationDTO(
    override val id: UUID = UUID.randomUUID(),
    val sortimentKategori: String,
    val postId: UUID,
    val status: PaakrevdGodkjenningskursStatus = PaakrevdGodkjenningskursStatus.ACTIVE,
    override val updatedByUser: String = "system",
    val createdByUser: String = "system",
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val deactivated: LocalDateTime? = null
): EventPayload {
    override fun toRapidDTO(): RapidDTO = PaakrevdGodkjenningskursRegistrationRapidDTO(
        id = id, sortimentKategori = sortimentKategori, postId = postId, status = status,
        updatedByUser = updatedByUser, createdByUser = createdByUser, created = created, updated = updated,
        deactivated = deactivated
    )
}

fun PaakrevdGodkjenningskursRegistration.toDTO(): PaakrevdGodkjenningskursRegistrationDTO = PaakrevdGodkjenningskursRegistrationDTO(
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

fun PaakrevdGodkjenningskursRegistrationDTO.toEntity(): PaakrevdGodkjenningskursRegistration = PaakrevdGodkjenningskursRegistration(
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
