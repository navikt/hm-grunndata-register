package no.nav.hm.grunndata.register.series

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.annotation.Version
import io.micronaut.data.model.DataType
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.CompatibleWith
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.IsoCategoryDTO
import no.nav.hm.grunndata.rapid.dto.RapidDTO
import no.nav.hm.grunndata.rapid.dto.SeriesAttributes
import no.nav.hm.grunndata.rapid.dto.SeriesData
import no.nav.hm.grunndata.rapid.dto.SeriesRegistrationRapidDTO
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.HMDB
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.event.EventPayload
import no.nav.hm.grunndata.register.product.MediaInfoDTO
import no.nav.hm.grunndata.register.product.ProductRegistrationDTOV2
import no.nav.hm.grunndata.register.product.toRapidMediaInfo
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID

@MappedEntity("series_reg_v1")
data class SeriesRegistration(
    @field:Id
    val id: UUID,
    val supplierId: UUID,
    val identifier: String,
    val title: String,
    val titleLowercase: String = title.lowercase(Locale.getDefault()),
    val text: String,
    val formattedText: String? = null,
    val isoCategory: String,
    @field:TypeDef(type = DataType.JSON)
    val seriesData: SeriesDataDTO,
    val message: String? = null,
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val status: SeriesStatus = SeriesStatus.ACTIVE,
    val adminStatus: AdminStatus = AdminStatus.PENDING,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val expired: LocalDateTime = LocalDateTime.now().plusYears(15),
    val published: LocalDateTime? = null,
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    val updatedByUser: String = "system",
    val createdByUser: String = "system",
    val createdByAdmin: Boolean = false,
    @field:GeneratedValue
    val count: Int = 0,
    @field:GeneratedValue
    val countDrafts: Int = 0,
    @field:GeneratedValue
    val countPublished: Int = 0,
    @field:GeneratedValue
    val countPending: Int = 0,
    @field:GeneratedValue
    val countDeclined: Int = 0,
    @field:Version
    val version: Long? = 0L,
)

data class SeriesDataDTO(
    val media: Set<MediaInfoDTO> = emptySet(),
    val attributes: SeriesAttributesDTO = SeriesAttributesDTO(),
)

data class SeriesAttributesDTO(
    val keywords: List<String>? = null,
    val url: String? = null,
    val compatibleWith: CompatibleWith? = null,
)

data class SeriesRegistrationDTO(
    override val id: UUID,
    val supplierId: UUID,
    val identifier: String,
    val title: String,
    val text: String,
    val formattedText: String? = null,
    val isoCategory: String,
    val message: String? = null,
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val adminStatus: AdminStatus = AdminStatus.PENDING,
    val status: SeriesStatus = SeriesStatus.ACTIVE,
    val seriesData: SeriesDataDTO,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val expired: LocalDateTime = LocalDateTime.now().plusYears(15),
    val published: LocalDateTime? = null,
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    override val updatedByUser: String = "system",
    val createdByUser: String = "system",
    val createdByAdmin: Boolean = false,
    val count: Int = 0,
    val countDrafts: Int = 0,
    val countPublished: Int = 0,
    val countPending: Int = 0,
    val countDeclined: Int = 0,
    val version: Long? = 0L,
    val titleLowercase: String = title.lowercase(Locale.getDefault()),
) : EventPayload {
    override fun toRapidDTO(): RapidDTO =
        SeriesRegistrationRapidDTO(
            id = id,
            supplierId = supplierId,
            identifier = identifier,
            title = title,
            text = text,
            formattedText = formattedText,
            isoCategory = isoCategory,
            draftStatus = draftStatus,
            adminStatus = adminStatus,
            status = status,
            created = created,
            updated = updated,
            published = published!!,
            expired = expired,
            createdBy = createdBy,
            updatedBy = updatedBy,
            updatedByUser = updatedByUser,
            createdByUser = createdByUser,
            createdByAdmin = createdByAdmin,
            seriesData = seriesData.toRapidDTO(),
            version = version,
        )

    fun isPublishedProduct(): Boolean {
        return draftStatus == DraftStatus.DONE &&
            adminStatus == AdminStatus.APPROVED &&
            status != SeriesStatus.DELETED &&
            published != null
    }
}

fun SeriesRegistration.toDTO() =
    SeriesRegistrationDTO(
        id = id,
        supplierId = supplierId,
        identifier = identifier,
        title = title,
        titleLowercase = titleLowercase,
        text = text,
        formattedText = formattedText,
        isoCategory = isoCategory,
        message = message,
        draftStatus = draftStatus,
        adminStatus = adminStatus,
        status = status,
        created = created,
        updated = updated,
        published = published,
        createdBy = createdBy,
        updatedBy = updatedBy,
        updatedByUser = updatedByUser,
        createdByUser = createdByUser,
        createdByAdmin = createdByAdmin,
        seriesData = seriesData,
        version = version,
        count = count,
        countDrafts = countDrafts,
        countPublished = countPublished,
        countPending = countPending,
        countDeclined = countDeclined,
        expired = expired,
    )

fun toSeriesRegistrationDTOV2(
    seriesRegistration: SeriesRegistration,
    supplierName: String,
    productRegistrationDTOs: List<ProductRegistrationDTOV2>,
    isoCategoryDTO: IsoCategoryDTO?,
    inAgreement: Boolean,
) = SeriesRegistrationDTOV2(
    id = seriesRegistration.id,
    supplierName = supplierName,
    title = seriesRegistration.title,
    text = seriesRegistration.text,
    isoCategory = isoCategoryDTO,
    message = seriesRegistration.message,
    status = EditStatus.from(seriesRegistration),
    seriesData = seriesRegistration.seriesData,
    created = seriesRegistration.created,
    updated = seriesRegistration.updated,
    published = seriesRegistration.published,
    expired = seriesRegistration.expired,
    updatedByUser = seriesRegistration.updatedByUser,
    createdByUser = seriesRegistration.createdByUser,
    variants = productRegistrationDTOs,
    version = seriesRegistration.version,
    isExpired = seriesRegistration.expired < LocalDateTime.now(),
    isPublished = seriesRegistration.published?.let { it < LocalDateTime.now() } ?: false,
    inAgreement = inAgreement,
    hmdbId =
        if (seriesRegistration.identifier != seriesRegistration.id.toString() &&
            seriesRegistration.updatedBy == HMDB
        ) {
            seriesRegistration.identifier
        } else {
            null
        },
)

fun SeriesRegistrationDTO.toEntity() =
    SeriesRegistration(
        id = id,
        supplierId = supplierId,
        identifier = identifier,
        title = title,
        text = text,
        formattedText = formattedText,
        isoCategory = isoCategory,
        message = message,
        draftStatus = draftStatus,
        status = status,
        adminStatus = adminStatus,
        created = created,
        updated = updated,
        published = published,
        createdBy = createdBy,
        seriesData = seriesData,
        updatedBy = updatedBy,
        updatedByUser = updatedByUser,
        version = version,
        count = count,
        countDrafts = countDrafts,
        countPublished = countPublished,
        countPending = countPending,
        countDeclined = countDeclined,
        expired = expired,
    )

fun SeriesDataDTO.toRapidDTO() =
    SeriesData(
        media = media.map { it.toRapidMediaInfo() }.toSet(),
        attributes =
            SeriesAttributes(
                keywords = attributes.keywords?.toSet(),
                url = attributes.url,
                compatibleWith = attributes.compatibleWith,
            ),
    )

enum class EditStatus {
    EDITABLE,
    PENDING_APPROVAL,
    REJECTED,
    DONE,
    ;

    companion object {
        fun from(seriesRegistration: SeriesRegistration): EditStatus {
            return if (seriesRegistration.adminStatus == AdminStatus.REJECTED) {
                REJECTED
            } else if (seriesRegistration.draftStatus == DraftStatus.DRAFT && seriesRegistration.adminStatus == AdminStatus.PENDING) {
                EDITABLE
            } else if (seriesRegistration.draftStatus == DraftStatus.DONE && seriesRegistration.adminStatus == AdminStatus.PENDING) {
                PENDING_APPROVAL
            } else if (seriesRegistration.adminStatus == AdminStatus.APPROVED) {
                DONE
            } else {
                throw IllegalArgumentException("Ukjent EditStatus for serie ${seriesRegistration.id}")
            }
        }
    }
}

data class UpdateSeriesRegistrationDTO(
    val title: String?,
    val text: String?,
    // val seriesData: SeriesDataDTO?,
)

data class SeriesRegistrationDTOV2(
    val id: UUID,
    val supplierName: String,
    val title: String,
    val text: String,
    val isoCategory: IsoCategoryDTO?,
    val message: String?,
    val status: EditStatus,
    val seriesData: SeriesDataDTO,
    val created: LocalDateTime,
    val updated: LocalDateTime,
    val published: LocalDateTime?,
    val expired: LocalDateTime,
    val updatedByUser: String,
    val createdByUser: String,
    val variants: List<ProductRegistrationDTOV2>,
    val version: Long?,
    val isExpired: Boolean,
    val isPublished: Boolean,
    val inAgreement: Boolean,
    val hmdbId: String? = null,
)
