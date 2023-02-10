package no.nav.hm.grunndata.register.product

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.annotation.Version
import io.micronaut.data.model.DataType
import jakarta.persistence.Column
import no.nav.hm.grunndata.dto.ProductDTO
import java.time.LocalDateTime
import java.util.*

@MappedEntity("product_reg_v1")
data class ProductRegistration(
    @field:Id
    val id: UUID,
    val supplierId: UUID,
    val supplierRef: String,
    @field:Column(name="hms_artnr")
    val HMSArtNr: String?,
    val title: String,
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val adminStatus: AdminStatus = AdminStatus.NOT_APPROVED,
    val status: RegistrationStatus = RegistrationStatus.ACTIVE,
    val message: String?=null,
    @field:TypeDef(type = DataType.JSON)
    val adminInfo: AdminInfo?=null,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val published: LocalDateTime?=null,
    val expired: LocalDateTime?=null,
    val updatedByUser: String="system",
    val createdByUser: String="system",
    val createdBy: String = "REGISTER",
    val updatedBy: String = "REGISTER",
    val createdByAdmin: Boolean = false,
    @field:TypeDef(type = DataType.JSON)
    val productDTO: ProductDTO,
    @field:Version
    val version: Long? = 0L)

fun ProductRegistration.isDraft(): Boolean = draftStatus == DraftStatus.DRAFT
fun ProductRegistration.isApproved(): Boolean = adminStatus == AdminStatus.APPROVED
fun ProductRegistration.approve(approvedByName: String): ProductRegistration =
    this.copy(adminInfo = AdminInfo(approvedBy = approvedByName), adminStatus = AdminStatus.APPROVED,
        status = RegistrationStatus.ACTIVE, draftStatus = DraftStatus.DONE, published = LocalDateTime.now())

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

const val REGISTER = "REGISTER"

data class ProductRegistrationDTO(
    val id: UUID = UUID.randomUUID(),
    val supplierId: UUID,
    val supplierRef: String,
    val HMSArtNr: String?,
    val title: String,
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val adminStatus: AdminStatus = AdminStatus.NOT_APPROVED,
    val status: RegistrationStatus = RegistrationStatus.ACTIVE,
    val message: String?,
    val adminInfo: AdminInfo?=null,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val published: LocalDateTime?,
    val expired: LocalDateTime?,
    val updatedByUser: String="system",
    val createdByUser: String="system",
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    val createdByAdmin: Boolean = false,
    val productDTO: ProductDTO,
    val version: Long?=null)
