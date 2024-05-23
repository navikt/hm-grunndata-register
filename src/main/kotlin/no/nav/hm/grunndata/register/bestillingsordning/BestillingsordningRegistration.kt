package no.nav.hm.grunndata.register.bestillingsordning

import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Id
import no.nav.hm.grunndata.rapid.dto.BestillingsordningRegistrationRapidDTO
import no.nav.hm.grunndata.rapid.dto.BestillingsordningStatus
import no.nav.hm.grunndata.rapid.dto.RapidDTO
import no.nav.hm.grunndata.register.event.EventPayload
import java.time.LocalDateTime
import java.util.*

@MappedEntity("bestillingsordning_reg_v1")
data class BestillingsordningRegistration(
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val hmsArtNr: String,
    val navn: String,
    val status: BestillingsordningStatus = BestillingsordningStatus.ACTIVE,
    val updatedByUser: String = "system",
    val createdByUser: String = "system",
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val deactivated: LocalDateTime? = null
)


data class BestillingsordningRegistrationDTO(
    override val id: UUID = UUID.randomUUID(),
    val hmsArtNr: String,
    val navn: String,
    val status: BestillingsordningStatus = BestillingsordningStatus.ACTIVE,
    override val updatedByUser: String = "system",
    val createdByUser: String = "system",
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val deactivated: LocalDateTime? = null
): EventPayload {
    override fun toRapidDTO(): RapidDTO = BestillingsordningRegistrationRapidDTO(
            id = id, hmsArtNr = hmsArtNr, navn = navn, status = status,
            updatedByUser = updatedByUser, createdByUser = createdByUser, created = created, updated = updated,
            deactivated = deactivated
        )

}

fun BestillingsordningRegistration.toDTO(): BestillingsordningRegistrationDTO = BestillingsordningRegistrationDTO(
    id = id,
    hmsArtNr = hmsArtNr,
    navn = navn,
    status = status,
    updatedByUser = updatedByUser,
    createdByUser = createdByUser,
    created = created,
    updated = updated,
    deactivated = deactivated
)

fun BestillingsordningRegistrationDTO.toEntity(): BestillingsordningRegistration = BestillingsordningRegistration(
    id = id,
    hmsArtNr = hmsArtNr,
    navn = navn,
    status = status,
    updatedByUser = updatedByUser,
    createdByUser = createdByUser,
    created = created,
    updated = updated,
    deactivated = deactivated
)