package no.nav.hm.grunndata.register.media

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import no.nav.hm.grunndata.rapid.dto.MediaDTO
import no.nav.hm.grunndata.rapid.dto.MediaType
import no.nav.hm.grunndata.register.media.UploadMediaController.Companion.V1_UPLOAD_MEDIA
import no.nav.hm.grunndata.register.security.Roles
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import java.util.*

@Secured(Roles.ROLE_ADMIN)
@Controller(V1_UPLOAD_MEDIA)
class UploadMediaController(private val mediaUploadClient: MediaUploadClient) {

    companion object {
        const val V1_UPLOAD_MEDIA = "/api/v1/admin/media"
        private val LOG = LoggerFactory.getLogger(UploadMediaController::class.java)
    }

    @Post(
        value = "/file/{oid}",
        consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA],
        produces = [io.micronaut.http.MediaType.APPLICATION_JSON]
    )
    suspend fun uploadFile(oid: UUID,
                           file: CompletedFileUpload,
                           authentication: Authentication): HttpResponse<MediaDTO> =
        HttpResponse.created(createMedia(file, oid))

    @Post(
        value = "/files/{oid}",
        consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA],
        produces = [io.micronaut.http.MediaType.APPLICATION_JSON]
    )
    suspend fun uploadFiles(oid: UUID,
                            files: Publisher<CompletedFileUpload>,
                            authentication: Authentication): HttpResponse<List<MediaDTO>> =
            HttpResponse.created(files.asFlow().map {createMedia(it, oid) }.toList())



    private suspend fun createMedia(file: CompletedFileUpload,
                                    oid: UUID): MediaDTO {
        val type = getMediaType(file)
        if (type == MediaType.OTHER) throw UknownMediaSource("only png, jpg, pdf is supported")
        val body = MultipartBody.builder().addPart(
            "file", file.filename,
            io.micronaut.http.MediaType.MULTIPART_FORM_DATA_TYPE, file.bytes
        ).build()
        LOG.info("upload media ${file.filename} for $oid")
        return mediaUploadClient.uploadFile(oid, body)
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
