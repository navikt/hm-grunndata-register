package no.nav.hm.grunndata.register.api.supplier

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import java.util.UUID

data class Supplier (
    @field:Id
    @field:GeneratedValue
    val uuid: UUID,
    val name: String,
    val address: String?=null,
    val homepage: String?=null,
    val phone: String?=null,
    val email: String,
    val identifier: String
)