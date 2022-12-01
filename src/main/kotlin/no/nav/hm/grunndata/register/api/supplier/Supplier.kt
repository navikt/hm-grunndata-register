package no.nav.hm.grunndata.register.api.supplier

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.util.UUID

@MappedEntity(SUPPLIER_V1)
data class Supplier (
    @field:Id
    val uuid: UUID = UUID.randomUUID(),
    val name: String,
    val address: String?=null,
    val homepage: String?=null,
    val phone: String?=null,
    val email: String,
    val identifier: String
)

const val SUPPLIER_V1= "supplier_v1"