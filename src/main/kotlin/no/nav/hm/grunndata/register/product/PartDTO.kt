package no.nav.hm.grunndata.register.product

import java.util.UUID

data class PartDTO (
    val id: UUID,
    val seriesUUID: UUID?,
    val hmsArtNr: String?,
    val supplierRef: String,
    val supplierName: String,
    val articleName: String,
    val accessory: Boolean,
    val sparePart: Boolean,
    val productData: ProductDataDTO,
)