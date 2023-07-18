package no.nav.hm.grunndata.register.supplier

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.SupplierDTO
import no.nav.hm.grunndata.rapid.dto.SupplierStatus
import no.nav.hm.grunndata.register.REGISTER
import java.awt.SystemColor.info
import java.time.LocalDateTime
import java.util.*

const val SUPPLIER_V1= "supplier_reg_v1"

@MappedEntity(SUPPLIER_V1)
data class SupplierRegistration (
    @field:Id
    val id: UUID,
    val status: SupplierStatus,
    val draftStatus: DraftStatus,
    val name: String,
    @field:TypeDef(type= DataType.JSON)
    val supplierData: SupplierData,
    val identifier: String,
    val created: LocalDateTime,
    val updated: LocalDateTime = LocalDateTime.now(),
    val createdBy: String,
    val updatedBy: String,
    val updatedByUser: String,
    val createdByUser: String
)

data class SupplierData(
    val address: String?=null,
    val postNr: String?=null,
    val postLocation: String?=null,
    val countryCode: String?=null,
    val email: String?=null,
    val phone: String?=null,
    val homepage: String?=null
)

data class SupplierRegistrationDTO (
    val id: UUID = UUID.randomUUID(),
    val status: SupplierStatus = SupplierStatus.ACTIVE,
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val name: String,
    @field:TypeDef(type= DataType.JSON)
    val supplierData: SupplierData,
    val identifier: String,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    val updatedByUser: String="system",
    val createdByUser: String="system",
)

fun SupplierRegistration.toDTO(): SupplierRegistrationDTO = SupplierRegistrationDTO (
    id = id, status = status, draftStatus = draftStatus, name = name, supplierData = supplierData,
    identifier = identifier, created = created, updated = updated, createdBy = createdBy, updatedBy = updatedBy,
    updatedByUser = updatedByUser, createdByUser = createdByUser
)


fun SupplierRegistrationDTO.toEntity(): SupplierRegistration = SupplierRegistration(
    id = id, status = status, name = name, supplierData = supplierData, identifier = identifier, created = created,
    updated = updated, createdBy = createdBy, updatedBy=updatedBy, createdByUser = createdByUser,
    draftStatus = draftStatus, updatedByUser = updatedByUser
)

