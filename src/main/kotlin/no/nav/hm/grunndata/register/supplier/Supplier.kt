package no.nav.hm.grunndata.register.supplier

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.util.UUID

const val SUPPLIER_V1= "supplier_v1"

@MappedEntity(SUPPLIER_V1)
data class Supplier (
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val status: SupplierStatus = SupplierStatus.ACTIVE,
    val name: String,
    val address: String?=null,
    val homepage: String?=null,
    val phone: String?=null,
    val email: String,
    val identifier: String
)


enum class SupplierStatus {
    INACTIVE, ACTIVE
}

data class SupplierDTO (
    val id: UUID,
    val status: SupplierStatus,
    val name: String,
    val address: String?=null,
    val homepage: String?=null,
    val phone: String?=null,
    val email: String,
    val identifier: String
)

fun Supplier.toDTO(): SupplierDTO = SupplierDTO (
    id = id, status = status, name = name, address = address, homepage = homepage, phone = phone, email = email,
    identifier = identifier)

fun SupplierDTO.toEntity(): Supplier = Supplier(
    id = id, status = status, name = name, address = address, homepage = homepage, phone = phone, email = email,
    identifier = identifier)

