package no.nav.hm.grunndata.register.user

import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class UserDTO (
    val id: UUID = UUID.randomUUID(),
    val name:String,
    val email: String,
    @field:TypeDef(type = DataType.JSON)
    val roles: List<String>,
    val attributes: Map<String, String>,
    val created: LocalDateTime,
    val updated: LocalDateTime
)

fun User.toDTO() = UserDTO (
    id = id,
    name = name,
    email = email,
    roles = roles,
    attributes = attributes,
    created = created,
    updated = updated
)