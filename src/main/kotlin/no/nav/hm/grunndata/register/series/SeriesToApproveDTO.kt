package no.nav.hm.grunndata.register.series

import java.time.LocalDateTime
import java.util.UUID

data class SeriesToApproveDTO(
    val title: String,
    val seriesUUID: UUID,
    val status: String,
    val supplierName: String,
    val firstImgUri: String? = null,
    val isExpired: Boolean,
    val updated: LocalDateTime,
    val mainProduct: Boolean
)
