package no.nav.hm.grunndata.register.productagreement

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef

import io.micronaut.data.model.DataType.JSON
import java.time.LocalDateTime
import java.util.*


@MappedEntity("product_agreement_registration_v1")
data class ProductAgreementRegistration(
    @field:Id
    val id: UUID=UUID.randomUUID(),
    val productId: UUID?=null,
    val supplierId: UUID,
    val supplierRef: String,
    val hmsArtNr: String,
    val agreementId: UUID,
    val reference: String,
    val post: Int,
    val rank: Int,
    val sparePartsOrAccessory: Boolean,
    @field:TypeDef(type=JSON)
    val info: ProductAgreementRegistrationInfo = ProductAgreementRegistrationInfo(),
    val status: ProductAgreementStatus = ProductAgreementStatus.ACTIVE,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val published: LocalDateTime = LocalDateTime.now(),
    val expired: LocalDateTime = LocalDateTime.now().plusYears(4)
)

data class ProductAgreementRegistrationInfo(val title: String?=null, val targetGroup: String?=null, val iso: String?=null)

enum class ProductAgreementStatus {
    ACTIVE,
    INACTIVE
}
