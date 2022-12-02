package no.nav.hm.grunndata.register.product

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.time.LocalDateTime
import java.util.*
import javax.persistence.Column

@MappedEntity("product_reg_v1")
data class ProductRegistration(
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val supplierUuid: UUID,
    val supplierRef: String,
    @field:Column(name="hms_artnr")
    val HMSArtNr: String?,
    val title: String,
    val draft: DraftStatus = DraftStatus.DRAFT,
    val adminStatus: AdminStatus = AdminStatus.NOT_APPROVED,
    val status: RegistrationStatus = RegistrationStatus.ACTIVE,
    val message: String,
    @field:TypeDef(type = DataType.JSON)
    val adminInfo: AdminInfo?=null,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val published: LocalDateTime?=null,
    val expired: LocalDateTime?=null,
    val createdBy: String,
    val updatedBy: String,
    val createdByAdmin: Boolean = false,
    @field:TypeDef(type = DataType.JSON)
    val productDTO: ProductDTO ) {
    fun isDraft(): Boolean = draft == DraftStatus.DRAFT
    fun isApproved(): Boolean = adminStatus == AdminStatus.APPROVED
}

enum class RegistrationStatus {
    INACTIVE, ACTIVE, DELETED
}

enum class AdminStatus {
    NOT_APPROVED, APPROVED
}

enum class DraftStatus {
    DRAFT, DONE
}

data class AdminInfo(val approvedBy: String?, val note: String?)

data class ProductDTO(
    val id: UUID = UUID.randomUUID(),
    val supplierUUID: String,
    val title: String,
    val description: Description,
    val status: ProductStatus = ProductStatus.ACTIVE,
    val HMSArtNr: String?=null,
    val identifier: String?=null,
    val supplierRef: String,
    val isoCategory: String,
    val accessory: Boolean = false,
    val sparepart: Boolean = false,
    val seriesId: String?=null,
    val techData: List<TechData> = emptyList(),
    val media: List<Media> = emptyList(),
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val expired: LocalDateTime = updated.plusYears(20),
    val agreementInfo: AgreementInfo?,
    val hasAgreement: Boolean = (agreementInfo!=null),
    val createdBy: String = "REGISTER",
    val updatedBy: String = "REGISTER"
)

data class Description(val name: String?=null,
                       val shortDescription: String?=null,
                       val text: String?=null)


data class AgreementInfo (
    val id: Long,
    val identifier: String?=null,
    val rank: Int,
    val postId: Long,
    val postNr: Int,
    val postIdentifier: String?=null,
    val reference: String?=null,
)


enum class ProductStatus {
    ACTIVE, INACTIVE
}

data class Media (
    val uuid:   UUID = UUID.randomUUID(),
    val order:  Int=1,
    val type: MediaType = MediaType.IMAGE,
    val uri:    String,
    val text:   String?=null,
    val source: MediaSourceType = MediaSourceType.ONPREM
)

enum class MediaSourceType {
    ONPREM, GCP, EXTERNALURL
}

enum class MediaType {
    PDF,
    IMAGE,
    VIDEO,
    OTHER
}

data class TechData (
    val key:    String,
    val value:  String,
    val unit:   String
)

data class ProductRegistrationDTO(
    val id: UUID,
    val supplierUuid: UUID,
    val supplierRef: String,
    val HMSArtNr: String?,
    val title: String,
    val draft: DraftStatus,
    val adminStatus: AdminStatus,
    val message: String,
    val adminInfo: AdminInfo?,
    val created: LocalDateTime,
    val updated: LocalDateTime,
    val published: LocalDateTime?,
    val expired: LocalDateTime?,
    val createdBy: String,
    val updatedBy: String,
    val createdByAdmin: Boolean,
    val productDTO: ProductDTO ) {
    fun isDraft(): Boolean = draft == DraftStatus.DRAFT
    fun isApproved(): Boolean = adminStatus == AdminStatus.APPROVED
}
