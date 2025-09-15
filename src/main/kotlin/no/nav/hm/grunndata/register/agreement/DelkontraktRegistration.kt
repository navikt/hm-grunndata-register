package no.nav.hm.grunndata.register.agreement

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import no.nav.hm.grunndata.rapid.dto.DelkontraktType
import no.nav.hm.grunndata.register.REGISTER
import java.time.LocalDateTime
import java.util.UUID

@MappedEntity("delkontrakt_reg_v1")
data class DelkontraktRegistration(
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val agreementId: UUID,
    val identifier: String = id.toString(),
    val type: DelkontraktType = DelkontraktType.WITH_DELKONTRAKT,
    @field:TypeDef(type = DataType.JSON)
    val delkontraktData: DelkontraktData,
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    val updated: LocalDateTime = LocalDateTime.now(),
)

data class DelkontraktData(
    val title: String? = null,
    val description: String? = null,
    val sortNr: Int = 0,
    var refNr: String? = null
) {
    init {
        refNr = extractDelkontraktNrFromTitle(title ?: "")
    }
}

data class DelkontraktRegistrationDTO(
    val id: UUID,
    val agreementId: UUID,
    val identifier: String,
    val type: DelkontraktType = DelkontraktType.WITH_DELKONTRAKT,
    val delkontraktData: DelkontraktData,
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    val updated: LocalDateTime = LocalDateTime.now(),
)

fun DelkontraktRegistration.toDTO() = DelkontraktRegistrationDTO(
    id = id,
    agreementId = agreementId,
    identifier = identifier,
    type = type,
    delkontraktData = delkontraktData,
    createdBy = createdBy,
    updatedBy = updatedBy,
    updated = updated
)

fun DelkontraktRegistrationDTO.toEntity() = DelkontraktRegistration(
    id = id,
    agreementId = agreementId,
    identifier = identifier,
    type = type,
    delkontraktData = delkontraktData,
    createdBy = createdBy,
    updatedBy = updatedBy,
    updated = updated
)

fun extractDelkontraktNrFromTitle(title: String): String? {
    val regex = """(\d+)([A-Z]*)([.|:])""".toRegex()
    return regex.find(title)?.groupValues?.get(0)?.dropLast(1)
}
