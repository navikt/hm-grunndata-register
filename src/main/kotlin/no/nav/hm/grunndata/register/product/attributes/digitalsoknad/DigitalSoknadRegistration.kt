package no.nav.hm.grunndata.register.product.attributes.digitalsoknad

import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Id
import no.nav.hm.grunndata.rapid.dto.DigitalSoknadRegistrationRapidDTO
import no.nav.hm.grunndata.rapid.dto.DigitalSoknadStatus
import no.nav.hm.grunndata.rapid.dto.RapidDTO
import no.nav.hm.grunndata.register.event.EventPayload
import java.time.LocalDateTime
import java.util.*

@MappedEntity("digitalsoknad_reg_v1")
data class DigitalSoknadRegistration(
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val hmsArtNr: String,
    val status: DigitalSoknadStatus = DigitalSoknadStatus.ACTIVE,
    val updatedByUser: String = "system",
    val createdByUser: String = "system",
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val deactivated: LocalDateTime? = null
)

data class DigitalSoknadRegistrationDTO(
    override val id: UUID = UUID.randomUUID(),
    val hmsArtNr: String,
    val status: DigitalSoknadStatus = DigitalSoknadStatus.ACTIVE,
    override val updatedByUser: String = "system",
    val createdByUser: String = "system",
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val deactivated: LocalDateTime? = null
): EventPayload {
    override fun toRapidDTO(): RapidDTO = DigitalSoknadRegistrationRapidDTO(
        id = id, hmsArtNr = hmsArtNr, status = status,
        updatedByUser = updatedByUser, createdByUser = createdByUser, created = created, updated = updated,
        deactivated = deactivated
    )
}

fun DigitalSoknadRegistration.toDTO(): DigitalSoknadRegistrationDTO = DigitalSoknadRegistrationDTO(
    id = id,
    hmsArtNr = hmsArtNr,
    status = status,
    updatedByUser = updatedByUser,
    createdByUser = createdByUser,
    created = created,
    updated = updated,
    deactivated = deactivated,
)

fun DigitalSoknadRegistrationDTO.toEntity(): DigitalSoknadRegistration = DigitalSoknadRegistration(
    id = id,
    hmsArtNr = hmsArtNr,
    status = status,
    updatedByUser = updatedByUser,
    createdByUser = createdByUser,
    created = created,
    updated = updated,
    deactivated = deactivated,
)
