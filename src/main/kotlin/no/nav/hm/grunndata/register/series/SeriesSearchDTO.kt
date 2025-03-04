package no.nav.hm.grunndata.register.series

import no.nav.hm.grunndata.register.product.MediaInfoDTO
import java.time.LocalDateTime
import java.util.UUID

data class SeriesSearchDTO(
    val title: String,
    val id: UUID,
    val status: EditStatus,
    val thumbnail: MediaInfoDTO? = null,
    val isExpired: Boolean,
    val isPublished: Boolean,
    val variantCount: Int,
    val updated: LocalDateTime,
    val updatedByUser: String,
    val mainProduct: Boolean,
)
