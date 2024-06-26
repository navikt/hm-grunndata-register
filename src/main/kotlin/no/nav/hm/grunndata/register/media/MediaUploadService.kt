package no.nav.hm.grunndata.register.media

import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.http.multipart.CompletedFileUpload
import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.MediaType
import org.slf4j.LoggerFactory
import java.util.*

@Singleton
class MediaUploadService(private val mediaUploadClient: MediaUploadClient) {

    companion object {
        private val LOG = LoggerFactory.getLogger(MediaUploadService::class.java)
    }

    suspend fun uploadMedia(file: CompletedFileUpload, oid: UUID, objectType: ObjectType): MediaDTO {
        val type = getMediaType(file)
        if (type == MediaType.OTHER) throw UknownMediaSource("only png, jpg, pdf is supported")
        val body = MultipartBody.builder().addPart(
            "file", file.filename,
            io.micronaut.http.MediaType.MULTIPART_FORM_DATA_TYPE, file.bytes
        ).build()
        LOG.info("upload media ${file.filename} for $oid")
        return mediaUploadClient.uploadFile(oid, objectType, body)
    }

    suspend fun getMediaList(oid: UUID): List<MediaDTO> = mediaUploadClient.getMediaList(oid)
        .filter { it.status == "ACTIVE" || it.status == "INACTIVE"  }

    suspend fun deleteByOidAndUri(oid: UUID, uri: String): MediaDTO? = mediaUploadClient.deleteByOidAndUri(oid, uri)

    private fun getMediaType(file: CompletedFileUpload): MediaType {
        return when (file.extension.lowercase()) {
            "jpg", "jpeg", "png" -> MediaType.IMAGE
            "pdf" -> MediaType.PDF
            else -> MediaType.OTHER
        }
    }

}

enum class ObjectType {
    SERIES,
    PRODUCT,
    AGREEMENT,
}