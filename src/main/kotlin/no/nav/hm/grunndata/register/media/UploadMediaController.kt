package no.nav.hm.grunndata.register.media

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import no.nav.hm.grunndata.rapid.dto.MediaDTO
import no.nav.hm.grunndata.rapid.dto.MediaType
import no.nav.hm.grunndata.register.media.UploadMediaController.Companion.V1_UPLOAD_MEDIA
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory
import java.util.*

@Secured(Roles.ROLE_ADMIN)
@Controller(V1_UPLOAD_MEDIA)
class UploadMediaController(private val mediaUploadClient: MediaUploadClient) {

    companion object {
        const val V1_UPLOAD_MEDIA = "/api/v1/admin/media/files"
        private val LOG = LoggerFactory.getLogger(UploadMediaController::class.java)
    }

    @Post(
        value = "/{oid}",
        consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA],
        produces = [io.micronaut.http.MediaType.APPLICATION_JSON]
    )
    suspend fun uploadFile(oid: UUID,
                           file: CompletedFileUpload,
                           authentication: Authentication): HttpResponse<MediaDTO> {
        return if (authentication.roles.contains(Roles.ROLE_ADMIN)) {
            val type = getMediaType(file)
            if (type == MediaType.OTHER) throw UknownMediaSource("only png, jpg, pdf is supported")
            val uri = "${oid}_${UUID.randomUUID()}.${file.extension}"
            LOG.info("Got file ${file.filename} with uri: $uri and size: ${file.size} for $oid")
            val body = MultipartBody.builder().addPart("file", file.filename,
                io.micronaut.http.MediaType.MULTIPART_FORM_DATA_TYPE, file.bytes
                ).build()
            val response = mediaUploadClient.uploadFile(oid, body)
            HttpResponse.created(response.body())
        } else {
            LOG.error("User is unauthorized ${authentication.roles}")
            HttpResponse.unauthorized()
        }
    }

    private fun getMediaType(file: CompletedFileUpload): MediaType {
        return when (file.extension.lowercase()) {
            "jpg", "jpeg", "png" -> MediaType.IMAGE
            "pdf" -> MediaType.PDF
            else -> MediaType.OTHER
        }
    }

}

val CompletedFileUpload.extension: String
    get() = filename.substringAfterLast('.', "")
