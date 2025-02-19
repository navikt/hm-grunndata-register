package no.nav.hm.grunndata.register.iso

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import no.nav.hm.grunndata.rapid.dto.IsoCategoryDTO
import no.nav.hm.grunndata.rapid.dto.IsoTranslationsDTO
import no.nav.hm.grunndata.register.REGISTER
import java.time.LocalDateTime


@MappedEntity("isocategory_reg_v1")
data class IsoCategoryRegistration(
    @field:Id
    val isoCode: String,
    val isoTitle: String,
    val isoText: String,
    val isoTextShort: String,
    @field:TypeDef(type = DataType.JSON)
    val isoTranslations: IsoTranslations = IsoTranslations(),
    val isoLevel: Int,
    val isActive: Boolean = true,
    val showTech: Boolean = true,
    val allowMulti: Boolean = true,
    @field:TypeDef(type = DataType.JSON)
    val searchWords: List<String> = emptyList(),
    val createdByUser: String,
    val updatedByUser: String,
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now()
)

data class IsoTranslations(
    val titleEn: String? = null,
    val textEn: String? = null
)

data class IsoCategoryRegistrationDTO(
    val isoCode: String,
    val isoTitle: String,
    val isoText: String,
    val isoTextShort: String,
    val isoTranslations: IsoTranslations = IsoTranslations(),
    val isoLevel: Int,
    val isActive: Boolean = true,
    val showTech: Boolean = true,
    val allowMulti: Boolean = true,
    val searchWords: List<String> = emptyList(),
    val createdByUser: String=REGISTER,
    val updatedByUser: String=REGISTER,
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now()
)

fun IsoCategoryRegistration.toRapidDTO(): IsoCategoryDTO = IsoCategoryDTO(
    isoCode = isoCode,
    isoTitle = isoTitle,
    isoText = isoText,
    isoTextShort = isoTextShort,
    isoTranslations = IsoTranslationsDTO(titleEn = isoTranslations.titleEn, textEn = isoTranslations.textEn),
    isoLevel = isoLevel,
    isActive = isActive,
    showTech = showTech,
    allowMulti = allowMulti
)

fun IsoCategoryRegistration.toDTO(): IsoCategoryRegistrationDTO = IsoCategoryRegistrationDTO(
    isoCode = isoCode,
    isoTitle = isoTitle,
    isoText = isoText,
    isoTextShort = isoTextShort,
    isoTranslations = isoTranslations,
    isoLevel = isoLevel,
    isActive = isActive,
    showTech = showTech,
    allowMulti = allowMulti,
    createdByUser = createdByUser,
    updatedByUser = updatedByUser,
    createdBy = createdBy,
    updatedBy = updatedBy,
    created = created,
    updated = updated,
    searchWords = searchWords
)

fun IsoCategoryRegistrationDTO.toEntity(): IsoCategoryRegistration = IsoCategoryRegistration(
    isoCode = isoCode,
    isoTitle = isoTitle,
    isoText = isoText,
    isoTextShort = isoTextShort,
    isoTranslations = isoTranslations,
    isoLevel = isoLevel,
    isActive = isActive,
    showTech = showTech,
    allowMulti = allowMulti,
    createdByUser = createdByUser,
    updatedByUser = updatedByUser,
    createdBy = createdBy,
    updatedBy = updatedBy,
    created = created,
    updated = updated,
    searchWords = searchWords
)