package no.nav.hm.grunndata.register.api.supplier

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.util.UUID

@MappedEntity(USER_V1)
data class User (
    @field:GeneratedValue
    @field:Id
    var id: UUID = UUID.randomUUID(),
    val name:String,
    val email: String,
    val supplierUuid: UUID,
    val token: String
)

const val USER_V1 = "user_v1"