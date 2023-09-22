package no.nav.hm.grunndata.register.series

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Version
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.register.REGISTER
import java.time.LocalDateTime
import java.util.*

@MappedEntity("series_reg_v1")
data class SeriesRegistration (

    @field:Id
    val id: UUID,
    val supplierId:UUID,
    val identifier: String,
    val name: String,
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val registrationStatus: RegistrationStatus = RegistrationStatus.ACTIVE,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    val updatedByUser: String = "system",
    val createdByUser: String = "system",
    val createdByAdmin: Boolean = false,
    @field:Version
    val version: Long? = 0L
)

