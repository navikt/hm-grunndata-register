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
import no.nav.hm.grunndata.register.api.BadRequestException
import no.nav.hm.grunndata.register.media.UploadMediaAdminController.Companion.API_V1_ADMIN_UPLOAD_MEDIA
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.security.Roles
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import java.util.*

@Secured(Roles.ROLE_ADMIN)
@Controller(API_V1_ADMIN_UPLOAD_MEDIA)
class UploadMediaAdminController(private val mediaUploadClient: MediaUploadClient,
                                 private val productRegistrationService: ProductRegistrationService,
                                 private val agreementRegistrationService: ProductRegistrationService) {

    companion object {
        const val API_V1_ADMIN_UPLOAD_MEDIA = "/api/v1/admin/media"
        private val LOG = LoggerFactory.getLogger(UploadMediaAdminController::class.java)
    }

    @Post(
        value = "/{type}/file/{oid}",
        consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA],
        produces = [io.micronaut.http.MediaType.APPLICATION_JSON]
    )
    suspend fun uploadFile(oid: UUID,
                           type: String,
                           file: CompletedFileUpload,
                           authentication: Authentication): HttpResponse<MediaDTO> {
        if (typeExists(type, oid)) {
            return HttpResponse.created(createMedia(file, oid))
        }
        throw BadRequestException("Unknown oid, must be of product or agreement")
    }

    @Post(
        value = "/{type}/files/{oid}",
        consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA],
        produces = [io.micronaut.http.MediaType.APPLICATION_JSON]
    )
    suspend fun uploadFiles(oid: UUID,
                            type: String,
                            files: Publisher<CompletedFileUpload>,
                            authentication: Authentication): HttpResponse<List<MediaDTO>>  {
        if (typeExists(type,oid)) {
            return HttpResponse.created(files.asFlow().map {createMedia(it, oid) }.toList())
        }
        throw BadRequestException("Unknown oid, must be of product or agreement")
    }



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

    private suspend fun typeExists(type: String, oid: UUID) =
        ("product" == type && productRegistrationService.findById(oid) != null
                || "agreement" == type && agreementRegistrationService.findById(oid) != null)
}
