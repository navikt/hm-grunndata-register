package no.nav.hm.grunndata.register.product

import no.nav.hm.grunndata.rapid.dto.Attributes
import no.nav.hm.grunndata.rapid.dto.MediaInfo
import no.nav.hm.grunndata.rapid.dto.ProductRapidDTO
import no.nav.hm.grunndata.rapid.dto.TechData


data class ProductData (
    val identifier: String? = null,
    val seriesIdentifier: String?= null,
    val attributes: Attributes = Attributes(),
    val accessory: Boolean = false,
    val sparePart: Boolean = false,
    val techData: List<TechData> = emptyList(),
    val media: Set<MediaInfo> = emptySet()
)



fun ProductRapidDTO.toProductData(): ProductData = ProductData (
    accessory = accessory,
    sparePart = sparePart,
    techData = techData,
    media = media,
    identifier = identifier,
    seriesIdentifier = seriesIdentifier,
    attributes = attributes
)
