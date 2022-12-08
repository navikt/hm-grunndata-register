package no.nav.hm.grunndata.register.user

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.util.UUID

@MappedEntity(USER_V1)
data class User (
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val name:String,
    val email: String,
    val token: String,
    @field:TypeDef(type = DataType.JSON)
    val roles: List<String> = emptyList(),
    @field:TypeDef(type = DataType.JSON)
    val attributes: Map<String, Any> = emptyMap(),
)

const val USER_V1 = "user_v1"

object UserAttribute {
    const val SUPPLIER_ID = "SUPPLIER_ID"
}