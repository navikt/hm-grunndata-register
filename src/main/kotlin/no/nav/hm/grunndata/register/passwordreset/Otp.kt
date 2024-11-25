package no.nav.hm.grunndata.register.passwordreset

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.LocalDateTime
import java.util.UUID

@MappedEntity("otp_v1")
data class Otp(
    @field:Id
    val id: UUID,
    val otp: String,
    val email: String,
    val used: Boolean,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
)
