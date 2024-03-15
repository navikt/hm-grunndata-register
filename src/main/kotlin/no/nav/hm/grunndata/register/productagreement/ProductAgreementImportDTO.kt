package no.nav.hm.grunndata.register.productagreement

data class ProductAgreementImportDTO(
    val dryRun: Boolean,
    val count: Int,
    val productAgreements: List<ProductAgreementRegistrationDTO>,
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
