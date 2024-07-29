package no.nav.hm.grunndata.register.productagreement

import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.series.SeriesRegistration

data class ProductAgreementImportDTO(
    val dryRun: Boolean,
    val count: Int,
    val newCount: Int,
    val file: String,
    val createdSeries: List<SeriesRegistration> = emptyList(),
    val createdAccessoryParts: List<ProductRegistration> = emptyList(),
    val createdMainProducts: List<ProductRegistration> = emptyList(),
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



