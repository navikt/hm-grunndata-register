package no.nav.hm.grunndata.register.techlabel

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import no.nav.hm.grunndata.register.REGISTER
import java.time.LocalDateTime
import java.util.*


@MappedEntity("techlabel_reg_v1")
data class TechLabelRegistration(
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val identifier: String = UUID.randomUUID().toString(),
    val label: String,
    val guide: String,
    val isoCode: String,
    val type: String,
    val unit: String?,
    val isActive: Boolean = true,
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    val updatedByUser: String = "system",
    val createdByUser: String = "system",
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now()
)

data class TechLabelRegistrationDTO(
    val id: UUID = UUID.randomUUID(),
    val identifier: String = UUID.randomUUID().toString(),
    val label: String,
    val guide: String,
    val isoCode: String,
    val type: String,
    val unit: String? = null,
    val isActive: Boolean = true,
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    val updatedByUser: String = "system",
    val createdByUser: String = "system",
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now()
)

fun TechLabelRegistration.toDTO(): TechLabelRegistrationDTO = TechLabelRegistrationDTO(
    id = id,
    identifier = identifier,
    label = label,
    guide = guide,
    isoCode = isoCode,
    type = type,
    unit = unit,
    isActive = isActive,
    createdBy = createdBy,
    updatedBy = updatedBy,
    updatedByUser = updatedByUser,
    createdByUser = createdByUser,
    created = created,
    updated = updated
)

fun TechLabelRegistrationDTO.toEntity(): TechLabelRegistration = TechLabelRegistration(
    id = id,
    identifier = identifier,
    label = label,
    guide = guide,
    isoCode = isoCode,
    type = type,
    unit = unit,
    isActive = isActive,
    createdBy = createdBy,
    updatedBy = updatedBy,
    updatedByUser = updatedByUser,
    createdByUser = createdByUser,
    created = created,
    updated = updated
)


data class TechLabelDTO(
    val id: UUID,
    val identifier: String,
    val label: String,
    val guide: String,
    val isocode: String,
    val type: String,
    val unit: String?,
    val createdBy: String,
    val updatedBy: String,
    val created: LocalDateTime,
    val updated: LocalDateTime
)