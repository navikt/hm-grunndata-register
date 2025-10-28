package no.nav.hm.grunndata.register.product

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.annotation.Version
import io.micronaut.data.model.DataType
import io.micronaut.security.authentication.Authentication
import jakarta.persistence.Column
import java.time.LocalDateTime
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.AgreementInfo
import no.nav.hm.grunndata.rapid.dto.Attributes
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.ProductRapidDTO
import no.nav.hm.grunndata.rapid.dto.ProductRegistrationRapidDTO
import no.nav.hm.grunndata.rapid.dto.ProductStatus
import no.nav.hm.grunndata.rapid.dto.RapidDTO
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.SupplierDTO
import no.nav.hm.grunndata.rapid.dto.TechData
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.event.EventPayload
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationCache
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationDTO
import no.nav.hm.grunndata.register.techlabel.TechLabelDTO
import no.nav.hm.grunndata.register.techlabel.TechLabelType

@MappedEntity("product_reg_v1")
data class ProductRegistration(
    @field:Id
    val id: UUID,
    val supplierId: UUID,
    val supplierRef: String,
    val seriesUUID: UUID,
    @field:Column(name = "hms_artnr")
    val hmsArtNr: String?,
    @Deprecated("Use series isoCategory instead")
    val isoCategory: String = "0",
    @Deprecated("Use series title instead")
    val title: String = "Use series title",
    val articleName: String,
    val accessory: Boolean = false,
    val sparePart: Boolean = false,
    val mainProduct: Boolean = true,
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val adminStatus: AdminStatus = AdminStatus.PENDING,
    val registrationStatus: RegistrationStatus = RegistrationStatus.ACTIVE,
    val message: String? = null,
    @field:TypeDef(type = DataType.JSON)
    val adminInfo: AdminInfo? = null,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val published: LocalDateTime? = null,
    val expired: LocalDateTime? = LocalDateTime.now().plusYears(5),
    val updatedByUser: String = "system",
    val createdByUser: String = "system",
    val createdBy: String = "REGISTER",
    val updatedBy: String = "REGISTER",
    val createdByAdmin: Boolean = false,
    @field:TypeDef(type = DataType.JSON)
    val productData: ProductData,
    @field:Version
    val version: Long? = 0L,
) {

    fun canChangeSupplierRef(authentication: Authentication): Boolean = authentication.isAdmin() || published == null

    fun canChangeHmsArtNr(authentication: Authentication): Boolean = authentication.isAdmin()
}

data class AdminInfo(val approvedBy: String?, val note: String? = null, val approved: LocalDateTime? = null)

fun ProductRegistration.isDraft(): Boolean = draftStatus == DraftStatus.DRAFT

fun ProductRegistration.isApproved(): Boolean = adminStatus == AdminStatus.APPROVED

fun ProductRegistration.approve(approvedByName: String): ProductRegistration =
    this.copy(
        adminInfo = AdminInfo(approvedBy = approvedByName),
        adminStatus = AdminStatus.APPROVED,
        registrationStatus = RegistrationStatus.ACTIVE,
        draftStatus = DraftStatus.DONE,
        published = LocalDateTime.now(),
    )

data class ProductRegistrationDTO(
    override val id: UUID = UUID.randomUUID(),
    val supplierId: UUID,
    val supplierRef: String,
    val hmsArtNr: String?,
    val seriesUUID: UUID,
    @Deprecated("Use seriesUUID instead")
    val seriesId: String,
    @Deprecated("Use series isoCategory instead")
    val isoCategory: String="0",
    @Deprecated("Use series title instead")
    val title: String="Use series title",
    val articleName: String,
    val accessory: Boolean = false,
    val sparePart: Boolean = false,
    val mainProduct: Boolean = true,
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val adminStatus: AdminStatus = AdminStatus.PENDING,
    val registrationStatus: RegistrationStatus = RegistrationStatus.ACTIVE,
    val message: String? = null,
    val adminInfo: AdminInfo? = null,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val published: LocalDateTime? = null,
    val expired: LocalDateTime? = LocalDateTime.now().plusYears(5),
    override val updatedByUser: String = "system",
    val createdByUser: String = "system",
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    val createdByAdmin: Boolean = false,
    val productData: ProductData,
    val agreements: List<AgreementInfo> = emptyList(),
    val version: Long? = 0L
) : EventPayload {
    override fun toRapidDTO(): RapidDTO =
        ProductRegistrationRapidDTO(
            id = id,
            partitionKey = seriesUUID.toString(),
            draftStatus = draftStatus,
            adminStatus = adminStatus,
            registrationStatus = registrationStatus,
            message = message,
            created = created,
            updated = updated,
            published = published,
            expired = expired,
            createdBy = createdBy,
            updatedBy = updatedBy,
            createdByAdmin = createdByAdmin,
            version = version,
            productDTO = productData.toProductDTO(this),
        )

    private fun ProductData.toProductDTO(registration: ProductRegistrationDTO): ProductRapidDTO =
        ProductRapidDTO(
            id = registration.id,
            supplier =
                (
                        SupplierRegistrationCache[registration.supplierId] ?:
                        SupplierRegistrationDTO(
                            id = registration.supplierId,
                            name = "ukjent",
                            supplierData = SupplierData(),
                        )
                ).toRapidDTO() as SupplierDTO,
            supplierRef = registration.supplierRef,
            title = registration.title,
            articleName = registration.articleName,
            hmsArtNr = registration.hmsArtNr,
            identifier = registration.id.toString(),
            isoCategory = registration.isoCategory,
            accessory = registration.accessory,
            sparePart = registration.sparePart,
            mainProduct = registration.mainProduct,
            seriesUUID = registration.seriesUUID,
            seriesId = registration.seriesId,
            techData = techData,
            media = media.map { it.toRapidMediaInfo() }.toSet(),
            created = registration.created,
            updated = registration.updated,
            published = registration.published ?: LocalDateTime.now(),
            expired = registration.expired ?: LocalDateTime.now().plusYears(10),
            agreements = registration.agreements,
            hasAgreement = registration.agreements.isNotEmpty(),
            createdBy = registration.createdBy,
            updatedBy = registration.updatedBy,
            attributes = attributes,
            status = setCorrectStatusFor(registration),
        )

    private fun setCorrectStatusFor(registration: ProductRegistrationDTO): ProductStatus =
        if (registration.registrationStatus == RegistrationStatus.DELETED)
            ProductStatus.DELETED
        else if (
            registration.adminStatus != AdminStatus.APPROVED ||
            registration.draftStatus == DraftStatus.DRAFT ||
            LocalDateTime.now().isAfter(registration.expired)
        ) {
            ProductStatus.INACTIVE
        } else {
            ProductStatus.ACTIVE
        }
}

fun ProductRegistrationDTO.toEntity(): ProductRegistration =
    ProductRegistration(
        id = id,
        supplierId = supplierId,
        seriesUUID = seriesUUID,
        supplierRef = supplierRef,
        hmsArtNr = hmsArtNr,
        title = title,
        articleName = articleName,
        draftStatus = draftStatus,
        adminStatus = adminStatus,
        registrationStatus = registrationStatus,
        message = message,
        adminInfo = adminInfo,
        created = created,
        updated = updated,
        published = published,
        expired = expired,
        updatedByUser = updatedByUser,
        createdByUser = createdByUser,
        createdBy = createdBy,
        updatedBy = updatedBy,
        createdByAdmin = createdByAdmin,
        productData = productData,
        isoCategory = isoCategory,
        version = version,
        sparePart = sparePart,
        accessory = accessory,
        mainProduct = mainProduct
    )



data class ProductRegistrationDryRunDTO(
    val id: UUID?,
    val supplierId: UUID,
    val supplierRef: String,
    val hmsArtNr: String?,
    val seriesUUID: UUID?,
    val isoCategory: String,
    val title: String,
    val articleName: String,
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val adminStatus: AdminStatus = AdminStatus.PENDING,
    val registrationStatus: RegistrationStatus = RegistrationStatus.ACTIVE,
    val message: String? = null,
    val adminInfo: AdminInfo? = null,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val published: LocalDateTime? = null,
    val expired: LocalDateTime? = null,
    val updatedByUser: String = "system",
    val createdByUser: String = "system",
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    val createdByAdmin: Boolean = false,
    val productData: ProductData,
    val agreements: List<AgreementInfo> = emptyList(),
    val version: Long? = 0L,
)


data class UpdateProductRegistrationDTO(
    val hmsArtNr: String?,
    val articleName: String,
    val supplierRef: String,
    val productData: ProductDataDTO
)

data class ProductRegistrationDTOV2(
    val id: UUID,
    val seriesUUID: UUID?,
    val hmsArtNr: String?,
    val supplierRef: String,
    val articleName: String,
    val accessory: Boolean,
    val sparePart: Boolean,
    val created: LocalDateTime,
    val productData: ProductDataDTO,
    val agreements: List<AgreementInfo>,
    val version: Long?,
    val isExpired: Boolean,
    val isPublished: Boolean
)

data class ProductDataDTO(
    val attributes: Attributes = Attributes(),
    val techData: List<ExtendedTechDataDTO> = emptyList(),
)

data class ExtendedTechDataDTO(
    val key: String,
    val value: String,
    val unit: String,
    val type: TechDataType,
    val definition: String?,
    val options: List<String>?
) {
    fun toEntity(): TechData = TechData(
        key = key,
        value = when (
            value.lowercase()) {
            "ja" -> "JA"
            "nei" -> "NEI"
            else -> value
        }, unit = unit
    )
}

fun TechData.toExtendedDTO(techLabels: List<TechLabelDTO>): ExtendedTechDataDTO {
    val techLabel = techLabels.find { it.label == key } ?: throw IllegalArgumentException("Ukjent techLabel: $key")
    return ExtendedTechDataDTO(
        key = key,
        value = when (value.lowercase()) {
            "ja" -> "Ja"
            "nei" -> "Nei"
            else -> value
        },
        unit = unit,
        type = TechDataType.from(techLabel),
        definition = techLabel.definition,
        options = techLabel.options
    )
}

enum class TechDataType {
    NUMBER,
    BOOLEAN,
    TEXT,
    OPTIONS;

    companion object {
        fun from(techLabel: TechLabelDTO): TechDataType {
            return when (techLabel.type) {
                TechLabelType.N -> NUMBER
                TechLabelType.L -> BOOLEAN
                TechLabelType.C -> if (techLabel.options.isEmpty()) TEXT else OPTIONS
                else -> throw IllegalArgumentException("Ukjent TechDataType for techlabel $techLabel")
            }
        }
    }
}