package no.nav.hm.grunndata.register.bestillingsordning

import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Id
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

enum class BestillingsordningStatus {
    ACTIVE, INACTIVE
}

data class BestillingsordningRegistrationDTO(
    val id: UUID,
    val hmsArtNr: String,
    val navn: String,
    val status: BestillingsordningStatus = BestillingsordningStatus.ACTIVE,
    val updatedByUser: String = "system",
    val createdByUser: String = "system",
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val deactivated: LocalDateTime? = null
)

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