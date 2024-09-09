package no.nav.hm.grunndata.register.series

import no.nav.hm.grunndata.register.product.MediaInfoDTO
import java.util.UUID

data class SeriesToApproveDTO(
    val title: String,
    val seriesUUID: UUID,
    val status: String,
    val supplierName: String,
    val thumbnail: MediaInfoDTO? = null,
    val isExpired: Boolean
)
