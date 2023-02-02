package no.nav.hm.grunndata.register.product

import java.time.LocalDateTime
import java.util.*

data class ProductDTO(
    val id: UUID,
    val supplierId: UUID,
    val title: String,
    val attributes: Map<AttributeNames, Any>,
    val status: ProductStatus = ProductStatus.ACTIVE,
    val HMSArtNr: String?=null,
    val identifier: String,
    val supplierRef: String,
    val isoCategory: String,
    val accessory: Boolean = false,
    val sparePart: Boolean = false,
    val seriesId: String?=null,
    val techData: List<TechData> = emptyList(),
    val media: List<Media> = emptyList(),
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val published: LocalDateTime = LocalDateTime.now(),
    val expired: LocalDateTime = updated.plusYears(20),
    val agreementInfo: AgreementInfo?,
    val hasAgreement: Boolean = (agreementInfo!=null && agreementInfo.expired.isAfter(LocalDateTime.now())),
    val createdBy: String = REGISTER,
    val updatedBy: String = REGISTER
)

data class TechData (
    val key:    String,
    val value:  String,
    val unit:   String
)


data class AgreementInfo (
    val id: UUID,
    val identifier: String?=null,
    val rank: Int,
    val postNr: Int,
    val postIdentifier: String?=null,
    val reference: String?=null,
    val expired: LocalDateTime,
)

enum class ProductStatus {
    ACTIVE, INACTIVE
}

data class Media (
    val id:   UUID = UUID.randomUUID(),
    val order:  Int=1,
    val type: MediaType = MediaType.IMAGE,
    val uri:    String,
    val text:   String?=null,
    val source: MediaSourceType = MediaSourceType.HMDB
)

enum class MediaSourceType {
    HMDB, GCP, EXTERNALURL
}

enum class MediaType {
    PDF,
    IMAGE,
    VIDEO,
    OTHER
}

enum class AttributeNames(private val type: AttributeType) {

    manufacturer(AttributeType.STRING),
    articlename(AttributeType.STRING),
    compatible(AttributeType.LIST),
    series(AttributeType.LIST),
    keywords(AttributeType.LIST),
    shortdescription(AttributeType.HTML),
    text(AttributeType.HTML),
    url(AttributeType.URL),
    tags(AttributeType.LIST),
    bestillingsordning(AttributeType.BOOLEAN)

}

enum class AttributeType {
    STRING, HTML, URL, LIST, JSON, BOOLEAN
}

inline fun <reified T : Enum<T>> enumContains(name: String): Boolean {
    return T::class.java.enumConstants.any { it.name == name}
}
