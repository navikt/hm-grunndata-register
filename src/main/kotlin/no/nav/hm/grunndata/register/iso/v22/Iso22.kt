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
    val isoTitleShort: String?=null,
    val isoText: String,
    @field:TypeDef(type = DataType.JSON)
    val isoTranslations: IsoTranslations = IsoTranslations(),
    @field:TypeDef(type = DataType.JSON)
    val searchWords: List<String> = emptyList(),
    val createdByUser: String,
    val updatedByUser: String,
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now()
)

data class IsoMap(
    val code16: String?=null,
    val code22: String?=null,
)

data class Kode16Map(
    val titel16dk: String?=null,
    val titel16eng: String?=null,
    val kode16: String?=null,
    val std_nat: String,
    val konv: String,
    val betydning: String,
    val kode22: String?=null,
    val titel22eng: String?=null,
    val titel22dk: String?=null,
    val isoCode: String?=null,
    val isoTitle_no: String?=null,
    val isoText_no: String?=null,
    val isoTextShort_no: String?=null,
    val isoLevel: String?=null
)