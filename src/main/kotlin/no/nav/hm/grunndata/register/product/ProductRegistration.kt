package no.nav.hm.grunndata.register.product

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
    val id: UUID,
    val supplierId: UUID,
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
    val createdBy: String = "REGISTER",
    val updatedBy: String = "REGISTER",
    val createdByAdmin: Boolean = false,
    @field:TypeDef(type = DataType.JSON)
    val productDTO: ProductDTO )

fun ProductRegistration.isDraft(): Boolean = draft == DraftStatus.DRAFT
fun ProductRegistration.isApproved(): Boolean = adminStatus == AdminStatus.APPROVED
fun ProductRegistration.approve(approvedByName: String): ProductRegistration =
    this.copy(adminInfo = AdminInfo(approvedBy = approvedByName), adminStatus = AdminStatus.APPROVED,
        status = RegistrationStatus.ACTIVE, draft = DraftStatus.DONE, published = LocalDateTime.now())

enum class RegistrationStatus {
   ACTIVE, DELETED
}

enum class AdminStatus {
    NOT_APPROVED, APPROVED
}

enum class DraftStatus {
    DRAFT, DONE
}

data class AdminInfo(val approvedBy: String?, val note: String?=null)

data class ProductDTO(
    val id: UUID = UUID.randomUUID(),
    val supplierId: UUID,
    val title: String,
    val description: Description,
    val status: ProductStatus = ProductStatus.INACTIVE,
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
    val published: LocalDateTime?=null,
    val expired: LocalDateTime?=null,
    val agreementInfo: AgreementInfo?,
    val hasAgreement: Boolean = (agreementInfo!=null),
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER
)

const val REGISTER = "REGISTER"

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
    // HMDB means it is stored in hjelpemiddeldatabasen
    HMDB, ONPREM, GCP, EXTERNALURL
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
    val supplierId: UUID,
    val supplierRef: String,
    val HMSArtNr: String?,
    val title: String,
    val draft: DraftStatus,
    val adminStatus: AdminStatus,
    val status: RegistrationStatus,
    val message: String,
    val adminInfo: AdminInfo?,
    val created: LocalDateTime,
    val updated: LocalDateTime,
    val published: LocalDateTime?,
    val expired: LocalDateTime?,
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    val createdByAdmin: Boolean,
    val productDTO: ProductDTO )
