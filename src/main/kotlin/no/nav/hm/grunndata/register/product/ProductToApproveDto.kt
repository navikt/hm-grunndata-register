package no.nav.hm.grunndata.register.product

import java.util.UUID

data class ProductToApproveDto(
    val title: String,
    val articleName: String,
    val seriesId: UUID,
    val status: String,
    val supplierName: String,
    val agreementId: UUID? = null,
    val delkontrakttittel: String? = null,
    val thumbnail: MediaInfoDTO? = null,
)
