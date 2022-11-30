package no.nav.hm.grunndata.register.api.supplier

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.util.UUID

@MappedEntity
data class User (
    @field:GeneratedValue
    @field:Id
    val uuid: UUID,
    val name:String,
    val email: String,
    val supplierUuid: UUID,
    val token: String
)

