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
    @field:TypeDef(type = DataType.JSON)
    val delkontraktData: DelkontraktData,
    val createdBy: String,
    val updatedBy: String,
    val updated: LocalDateTime = LocalDateTime.now(),
)

data class DelkontraktData(
    val identifier: String?=null,
    val title: String?=null,
    val description: String?=null,
    val sortNr: Int=0,
    val delKontraktRefNr: String?=null // "1" eller "1A"
)
