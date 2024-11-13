package no.nav.hm.grunndata.register.productagreement

import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.series.SeriesRegistration

data class ProductAgreementImportDTO(
    val dryRun: Boolean,
    @Deprecated("remove this later, confusing")
    val count: Int,
    @Deprecated("remove this later, confusing")
    val newCount: Int,
    val file: String,
    val createdSeries: List<SeriesRegistration> = emptyList(),
    val createdAccessoryParts: List<ProductRegistration> = emptyList(),
    val createdMainProducts: List<ProductRegistration> = emptyList(),
    val newProductAgreements: List<ProductAgreementRegistrationDTO> = emptyList(),
    val updatedAgreements: List<ProductAgreementRegistrationDTO> = emptyList(),
    val deactivatedAgreements: List<ProductAgreementRegistrationDTO> = emptyList(),
    @Deprecated("No need for this field anymore")
    val productAgreementsWithInformation: List<Pair<ProductAgreementRegistrationDTO, List<Information>>> = emptyList()
)

data class Information(
    val message: String,
    val type: Type,
)

enum class Type {
    INFO,
    WARNING,
}



