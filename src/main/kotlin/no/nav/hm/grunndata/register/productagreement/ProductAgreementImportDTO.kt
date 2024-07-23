package no.nav.hm.grunndata.register.productagreement

import no.nav.hm.grunndata.register.product.ProductRegistration

data class ProductAgreementImportDTO(
    val dryRun: Boolean,
    val count: Int,
    val file: String,
    val productAgreementsWithInformation: List<Pair<ProductAgreementRegistrationDTO, List<Information>>>,
)

data class Information(
    val message: String,
    val type: Type,
)

enum class Type {
    INFO,
    WARNING,
}

data class CreatedProductRegistrationDTO(
    val productRegistration: ProductRegistration
)
