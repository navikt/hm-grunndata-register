package no.nav.hm.grunndata.register.media

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import no.nav.hm.grunndata.rapid.dto.MediaDTO
import no.nav.hm.grunndata.register.api.BadRequestException
import no.nav.hm.grunndata.register.media.UploadMediaController.Companion.API_V1_UPLOAD_MEDIA
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.supplierId
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import java.util.*

@Secured(Roles.ROLE_SUPPLIER)
@Controller(API_V1_UPLOAD_MEDIA)
class UploadMediaController(private val mediaUploadService: MediaUploadService,
                            private val productRegistrationService: ProductRegistrationService) {

    companion object {
        const val API_V1_UPLOAD_MEDIA = "/api/v1/media"
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
        if (typeExists(type, oid, authentication.supplierId())) {
            return HttpResponse.created(mediaUploadService.uploadMedia(file, oid))
        }
        throw BadRequestException("Wrong id?")
    }

    @Get("/{oid}")
    suspend fun getMediaList(oid:UUID, authentication: Authentication): HttpResponse<List<MediaDTO>> {
        if (productRegistrationService.findByIdAndSupplierId(oid, authentication.supplierId())!=null) {
            return HttpResponse.ok(mediaUploadService.getMediaList(oid))
        }
        throw BadRequestException("Wrong id?")
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
        if (typeExists(type, oid, authentication.supplierId())) {
            return HttpResponse.created(files.asFlow().map {mediaUploadService.uploadMedia(it, oid) }.toList())
        }
        throw BadRequestException("Wrong id?")
    }

    private suspend fun typeExists(type: String, oid: UUID, supplierId: UUID) =
        ("product" == type && productRegistrationService.findByIdAndSupplierId(oid, supplierId) != null)
}


val CompletedFileUpload.extension: String
    get() = filename.substringAfterLast('.', "")

