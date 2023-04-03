package no.nav.hm.grunndata.register.product

import no.nav.hm.grunndata.rapid.dto.*


data class ProductData (
    val attributes: Attributes = Attributes(),
    val isoCategory: String,
    val accessory: Boolean = false,
    val sparePart: Boolean = false,
    val seriesId: String?,
    val techData: List<TechData> = emptyList(),
    val media: List<MediaInfo> = emptyList(),
    val agreementInfo: AgreementInfo?=null,
)



fun ProductDTO.toProductData(): ProductData = ProductData (
    isoCategory = isoCategory,
    accessory = accessory,
    sparePart = sparePart,
    seriesId = seriesId,
    techData = techData,
    media = media,
    agreementInfo = agreementInfo,
    attributes = attributes
)
