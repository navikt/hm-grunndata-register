package no.nav.hm.grunndata.register.product

import no.nav.hm.grunndata.rapid.dto.Attributes
import no.nav.hm.grunndata.rapid.dto.MediaInfo
import no.nav.hm.grunndata.rapid.dto.MediaSourceType
import no.nav.hm.grunndata.rapid.dto.MediaType
import no.nav.hm.grunndata.rapid.dto.ProductRapidDTO
import no.nav.hm.grunndata.rapid.dto.TechData
import java.time.LocalDateTime


data class ProductData (
    val attributes: Attributes = Attributes(),
    val accessory: Boolean = false,
    val sparePart: Boolean = false,
    val techData: List<TechData> = emptyList(),
    @Deprecated("Use series media instead")
    val media: Set<MediaInfoDTO> = emptySet(),
    val identifier: String? = null,
    val seriesIdentifier: String? = null,
)



fun ProductRapidDTO.toProductData(): ProductData = ProductData (
    accessory = accessory,
    sparePart = sparePart,
    techData = techData,
    media = media.map { it.toMediaInfo() }.toSet(),
    attributes = attributes,
    identifier = identifier,
    seriesIdentifier = seriesIdentifier,
)

fun MediaInfoDTO.toRapidMediaInfo() = MediaInfo (
    sourceUri = sourceUri,
    filename = filename,
    uri = uri,
    priority = priority,
    type = type,
    text = text,
    source = source,
    updated = updated?: LocalDateTime.now()
)

fun MediaInfo.toMediaInfo() = MediaInfoDTO (
    sourceUri = sourceUri,
    filename = filename,
    uri = uri,
    priority = priority,
    type = type,
    text = text,
    source = source,
    updated = updated
)


data class MediaInfoDTO (
    val sourceUri: String,
    val filename: String?=null,
    val uri:    String,
    val priority: Int = -1,
    val type: MediaType = MediaType.IMAGE,
    val text:   String?=null,
    val source: MediaSourceType = MediaSourceType.HMDB,
    val updated: LocalDateTime? = LocalDateTime.now(),
) {
    override fun hashCode(): Int {
        return uri.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MediaInfoDTO) return false
        return uri == other.uri
    }
}
