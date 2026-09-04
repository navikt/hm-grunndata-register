package no.nav.hm.grunndata.register.iso.v22

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.time.LocalDateTime
import java.util.UUID

@MappedEntity("iso_map_v22")
data class IsoMap(
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val code16: String="",
    @field:TypeDef(type = DataType.JSON)
    val mapEnum: List<IsoMapEnum> = emptyList(),
    val code22: String="",
    val created: LocalDateTime = LocalDateTime.now(),
    val verified: Boolean = false,
)

enum class IsoMapEnum(val code: Char, val textNo: String) {
    SAME('=', "Ingenting er endret"),
    CHANGED_CODE_SAME_HEADER('C', "Endret kode, samme overskrift"),
    CHANGED_CODE_CHANGED_HEADER('~', "Endret kode, endret overskrift"),
    SAME_CODE_CHANGED_HEADER('#', "Samme kode, endret overskrift"),
    NEW_CODE_NEW_HEADER_MERGED('>', "Ny kode, ny overskrift, to eller flere inndelinger slått sammen"),
    SAME_OR_CHANGED_CODE_SAME_HEADER_MERGED('≥', "Samme kode eller endret kode, samme overskrift, to eller flere inndelinger slått sammen"),
    CHANGED_EXPLANATION('*', "Endret forklaring"),
    ADDED_EXPLANATION('+', "Tilføyd forklaring"),
    NEW_CODE_NEW_HEADER_SPLIT('<', "Ny kode, ny overskrift — Består av deler av tidligere inndelinger som har blitt delt opp"),
    NEW_CODE_SAME_HEADER_SPLIT('≤', "Ny kode, samme overskrift — Består av deler av tidligere inndelinger som har blitt delt opp"),
    DELETED_CLASS_OR_SUBCLASS_OR_SECTION('X', "Klasse/underklasse eller inndeling har blitt slettet"),
    NEW_CLASS_OR_SUBCLASS_OR_SECTION('!', "Ny klasse/underklasse eller inndeling"),
    SAME_CODE_NEW_HEADER_AND_EXPLANATION('±', "Samme kode, ny overskrift og ny forklaring"),
    UNKNOWN('?', "Ukjent kode");

    companion object {
        fun fromCode(code: String): List<IsoMapEnum> = code.trim().mapNotNull { char -> entries.find { it.code == char } }
    }
}

