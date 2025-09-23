package no.nav.hm.grunndata.register.techlabel

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import no.nav.hm.grunndata.register.REGISTER
import java.time.LocalDateTime
import java.util.*


@MappedEntity("techlabel_reg_v1")
data class TechLabelRegistration(
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val identifier: String = UUID.randomUUID().toString(),
    val label: String,
    val guide: String? = label,
    val definition: String?,
    val isoCode: String,
    val type: TechLabelType,
    val unit: String?,
    val sort: Int,
    @field:TypeDef(type = DataType.JSON)
    val options: List<String> = emptyList(),
    val isActive: Boolean = true,
    val isKeyLabel: Boolean? = false,
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
    val guide: String ? = label,
    val definition: String?,
    val isoCode: String,
    val type: TechLabelType,
    val unit: String? = null,
    val sort: Int,
    val options: List<String> = emptyList(),
    val isActive: Boolean = true,
    val isKeyLabel: Boolean? = false,
    val systemLabel: String = label.systemLabel(type),
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
    definition = definition,
    isoCode = isoCode,
    type = type,
    unit = unit,
    sort = sort,
    options = options,
    isActive = isActive,
    isKeyLabel = isKeyLabel,
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
    definition = definition,
    isoCode = isoCode,
    type = type,
    unit = unit,
    sort = sort,
    options = options,
    isActive = isActive,
    isKeyLabel = isKeyLabel,
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
    val guide: String ? = label,
    val definition: String?,
    val isocode: String,
    val type: TechLabelType,
    val unit: String?,
    val sort: Int,
    val isKeyLabel: Boolean? = false,
    val systemLabel: String? = label.systemLabel(type),
    val options: List<String> = emptyList(),
    val createdBy: String,
    val updatedBy: String,
    val created: LocalDateTime,
    val updated: LocalDateTime
)

fun TechLabelRegistration.toTechLabelDTO(): TechLabelDTO = TechLabelDTO(
    id = id,
    identifier = identifier,
    label = label,
    guide = guide,
    definition = definition,
    isocode = isoCode,
    type = type,
    unit = unit,
    sort = sort,
    isKeyLabel = isKeyLabel,
    systemLabel = label.systemLabel(type),
    options = options,
    createdBy = createdBy,
    updatedBy = updatedBy,
    created = created,
    updated = updated
)
fun String.systemLabel(type: TechLabelType): String {
    val replaced = this.replace("æ", "ae")
        .replace("ø", "o")
        .replace("å", "a")
        .replace("[^A-Za-z]".toRegex(), "")+type
    return replaced.lowercase()
}

enum class TechLabelType {
    N, L, C
}