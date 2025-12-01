package no.nav.hm.grunndata.register.servicejob

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import no.nav.hm.grunndata.rapid.dto.ServiceAgreementStatus
import no.nav.hm.grunndata.register.REGISTER
import java.time.LocalDateTime
import java.util.UUID

@MappedEntity("service_agreement_v1")
data class ServiceAgreement(
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val serviceId: UUID,
    val supplierId: UUID,
    val supplierRef: String? = null,
    val agreementId: UUID,
    val status: ServiceAgreementStatus = ServiceAgreementStatus.INACTIVE,
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val published: LocalDateTime,
    val expired: LocalDateTime,
    val updatedByUser: String = "system",
)