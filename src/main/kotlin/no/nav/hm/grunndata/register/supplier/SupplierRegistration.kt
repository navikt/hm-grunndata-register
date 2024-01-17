package no.nav.hm.grunndata.register.supplier

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import no.nav.hm.grunndata.rapid.dto.*
import no.nav.hm.grunndata.rapid.event.RapidApp
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.event.EventPayload
import java.time.LocalDateTime
import java.util.*

const val SUPPLIER_V1= "supplier_reg_v1"

@MappedEntity(SUPPLIER_V1)
data class SupplierRegistration (
    @field:Id
    val id: UUID,
    val status: SupplierStatus = SupplierStatus.ACTIVE,
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val name: String,
    @field:TypeDef(type= DataType.JSON)
    val supplierData: SupplierData,
    val identifier: String,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val createdBy: String = RapidApp.grunndata_register,
    val updatedBy: String = RapidApp.grunndata_register,
    val updatedByUser: String = "admin",
    val createdByUser: String = "admin"
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
    override val id: UUID = UUID.randomUUID(),
    val status: SupplierStatus = SupplierStatus.ACTIVE,
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val name: String,
    val supplierData: SupplierData,
    val identifier: String = UUID.randomUUID().toString(),
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    override val updatedByUser: String="system",
    val createdByUser: String="system",
): EventPayload {
    override fun toRapidDTO(): RapidDTO = SupplierDTO (
        id = id, status = status, name=name, info = supplierData.toInfo() , identifier = identifier, created = created, updated = updated,
        createdBy = createdBy, updatedBy = updatedBy)


    private fun SupplierData.toInfo(): SupplierInfo = SupplierInfo (
        address = address, postNr = postNr, postLocation = postLocation, countryCode = countryCode, email = email,
        phone = phone, homepage = homepage
    )

}

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

