package no.nav.hm.grunndata.register.iso.v22

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.iso.IsoTranslations
import java.time.LocalDateTime
import java.util.UUID

@MappedEntity("iso_v22")
data class Iso22 (
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val isoCode: String,
    val isoTitle: String,
    val isoText: String,
    @field:TypeDef(type = DataType.JSON)
    val isoTranslations: IsoTranslations = IsoTranslations(),
    @field:TypeDef(type = DataType.JSON)
    val searchWords: List<String> = emptyList(),
    val isoType: IsoType = IsoType.ISO,
    val createdByUser: String,
    val updatedByUser: String,
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now()
)

enum class IsoType {
    ISO,
    NAT,
    OBS
}
