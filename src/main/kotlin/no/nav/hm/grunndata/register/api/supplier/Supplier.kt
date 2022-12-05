package no.nav.hm.grunndata.register.api.supplier

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.util.UUID

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

const val SUPPLIER_V1= "supplier_v1"