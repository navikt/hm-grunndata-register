package no.nav.hm.grunndata.register.product

import java.util.UUID

data class ProductToApproveDto(
    val title: String,
    val articleName: String,
    val productId: UUID,
    val seriesId: UUID,
    val status: String,
    val supplierName: String,
    val agreementId: UUID? = null,
    val delkontrakttittel: String? = null,
    val thumbnail: MediaInfoDTO? = null,
    val sparePart: Boolean = false,
    val accessory: Boolean = false,
)
