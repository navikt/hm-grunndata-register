package no.nav.hm.grunndata.register.productagreement

import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.series.SeriesRegistration

data class ProductAgreementImportDTO(
    val dryRun: Boolean,
    val count: Int,
    val file: String,
    val createdSeries: List<SeriesRegistration> = emptyList(), // new series created for approvement
    val createdAccessoryParts: List<ProductRegistration> = emptyList(), // new products created for approvement
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



