package no.nav.hm.grunndata.register.agreement

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.time.LocalDateTime
import java.util.*

@MappedEntity("delkontrakt_reg_v1")
data class DelkontraktRegistration(
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val agreementId: UUID,
    val identifier: String = id.toString(),
    @field:TypeDef(type = DataType.JSON)
    val delkontraktData: DelkontraktData,
    val createdBy: String,
    val updatedBy: String,
    val updated: LocalDateTime = LocalDateTime.now(),
)

data class DelkontraktData(
    val title: String?=null,
    val description: String?=null,
    val sortNr: Int=0,
    val refNr: String?=null // "1" eller "1A"
)

data class DelkontraktRegistrationDTO(
    val id: UUID = UUID.randomUUID(),
    val agreementId: UUID,
    val identifier: String = UUID.randomUUID().toString(),
    val delkontraktData: DelkontraktData,
    val createdBy: String,
    val updatedBy: String,
    val updated: LocalDateTime = LocalDateTime.now(),
)

fun DelkontraktRegistration.toDTO() = DelkontraktRegistrationDTO(
    id = id,
    agreementId = agreementId,
    identifier = identifier,
    delkontraktData = delkontraktData,
    createdBy = createdBy,
    updatedBy = updatedBy,
    updated = updated
)

fun DelkontraktRegistrationDTO.toEntity() = DelkontraktRegistration(
    id = id,
    agreementId = agreementId,
    identifier = identifier,
    delkontraktData = delkontraktData,
    createdBy = createdBy,
    updatedBy = updatedBy,
    updated = updated
)