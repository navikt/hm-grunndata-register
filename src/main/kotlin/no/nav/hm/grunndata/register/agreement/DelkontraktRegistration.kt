package no.nav.hm.grunndata.register.agreement

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.util.*

@MappedEntity("delkontrakt_reg_v1")
data class DelkontraktRegistration(
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val agreementId: UUID,
    val identifier: String,
    val title: String,
    val description: String,
    val sortNr: Int
)

