package no.nav.hm.grunndata.register.product

import no.nav.hm.grunndata.rapid.dto.*


data class ProductData (
    val attributes: Attributes = Attributes(),
    val accessory: Boolean = false,
    val sparePart: Boolean = false,
    val techData: List<TechData> = emptyList(),
    val media: List<MediaInfo> = emptyList(),
    @Deprecated("use agreements")
    val agreementInfo: AgreementInfo?=null,
    val agreements: List<AgreementInfo> = emptyList()
)



fun ProductRapidDTO.toProductData(): ProductData = ProductData (
    accessory = accessory,
    sparePart = sparePart,
    techData = techData,
    media = media,
    agreementInfo = agreementInfo,
    agreements = agreements,
    attributes = attributes
)
