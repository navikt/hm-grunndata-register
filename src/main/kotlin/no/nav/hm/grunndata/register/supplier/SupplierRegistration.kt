package no.nav.hm.grunndata.register.supplier

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import no.nav.hm.grunndata.rapid.dto.SupplierDTO
import no.nav.hm.grunndata.rapid.dto.SupplierInfo
import no.nav.hm.grunndata.rapid.dto.SupplierStatus
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
    val info: SupplierInfo,
    val identifier: String,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER
)

data class SupplierRegistration (
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val status: SupplierStatus = SupplierStatus.ACTIVE,
    val name: String,
    @field:TypeDef(type= DataType.JSON)
    val info: SupplierInfo,
    val identifier: String,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER
)
fun Supplier.toDTO(): SupplierDTO = SupplierDTO (
    id = id, status = status, name=name, info = info, identifier = identifier, created = created, updated = updated,
    createdBy = createdBy, updatedBy = updatedBy)

fun SupplierDTO.toEntity(): Supplier = Supplier(
    id = id, status = status, name = name, info = info, identifier = identifier, created = created, updated = updated,
    createdBy = createdBy, updatedBy=updatedBy)

