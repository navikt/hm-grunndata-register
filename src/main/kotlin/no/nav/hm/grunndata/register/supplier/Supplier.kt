package no.nav.hm.grunndata.register.supplier

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import no.nav.hm.grunndata.register.product.REGISTER
import java.time.LocalDateTime
import java.util.UUID

const val SUPPLIER_V1= "supplier_v1"

@MappedEntity(SUPPLIER_V1)
data class Supplier (
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val status: SupplierStatus = SupplierStatus.ACTIVE,
    val name: String,
    @field:TypeDef(type= DataType.JSON)
    val info:       SupplierInfo,
    val identifier: String,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER
)

data class SupplierInfo (
    val address: String?=null,
    val email: String?=null,
    val phone: String?=null,
    val homepage: String?=null,
    val deactivated: LocalDateTime?=null,
)

enum class SupplierStatus {
    INACTIVE, ACTIVE
}

data class SupplierDTO (
    val id: UUID,
    val status: SupplierStatus,
    val name: String,
    val info:   SupplierInfo,
    val identifier: String,
    val created: LocalDateTime,
    val updated: LocalDateTime,
    val createdBy: String,
    val updatedBy: String
)

fun Supplier.toDTO(): SupplierDTO = SupplierDTO (
    id = id, status = status, name=name, info = info, identifier = identifier, created = created, updated = updated,
    createdBy = createdBy, updatedBy = updatedBy)

fun SupplierDTO.toEntity(): Supplier = Supplier(
    id = id, status = status, name = name, info = info, identifier = identifier, created = created, updated = updated,
    createdBy = createdBy, updatedBy=updatedBy)

