package no.nav.hm.grunndata.register.media

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationService
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.media.MediaAdminController.Companion.API_V1_ADMIN_UPLOAD_MEDIA
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.security.Roles
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import java.util.*

@Secured(Roles.ROLE_ADMIN)
@Controller(API_V1_ADMIN_UPLOAD_MEDIA)
class MediaAdminController(private val mediaUploadService: MediaUploadService,
                           private val productRegistrationService: ProductRegistrationService,
                           private val agreementRegistrationService: AgreementRegistrationService) {

    companion object {
        const val API_V1_ADMIN_UPLOAD_MEDIA = "/admin/api/v1/media"
        private val LOG = LoggerFactory.getLogger(MediaAdminController::class.java)
    }

    @Post(
        value = "/product/files/{oid}",
        consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA],
        produces = [io.micronaut.http.MediaType.APPLICATION_JSON]
    )
    suspend fun uploadProductFiles(oid: UUID,
                            files: Publisher<CompletedFileUpload>,
                            authentication: Authentication): HttpResponse<List<MediaDTO>>  {
        if (productRegistrationService.findById(oid) != null) {
            return HttpResponse.created(files.asFlow().map {mediaUploadService.uploadMedia(it, oid) }.toList())
        }
        throw BadRequestException("Unknown oid, must be of product")
    }

    @Post(
        value = "/agreement/files/{oid}",
        consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA],
        produces = [io.micronaut.http.MediaType.APPLICATION_JSON]
    )
    suspend fun uploadAgreementFiles(oid: UUID,
                            files: Publisher<CompletedFileUpload>,
                            authentication: Authentication): HttpResponse<List<MediaDTO>>  {
        if (agreementRegistrationService.findById(oid) != null) {
            return HttpResponse.created(files.asFlow().map {mediaUploadService.uploadMedia(it, oid) }.toList())
        }
        throw BadRequestException("Unknown oid, must be of agreement")
    }

    @Get("/{oid}")
    suspend fun getMediaList(oid:UUID, authentication: Authentication): HttpResponse<List<MediaDTO>> {
        return HttpResponse.ok(mediaUploadService.getMediaList(oid))
    }

    @Delete("/{oid}/{uri}")
    suspend fun deleteFile(oid: UUID, uri: String, authentication: Authentication): HttpResponse<MediaDTO> {
        LOG.info("Deleting media file oid: $oid and $uri")
        return HttpResponse.ok(mediaUploadService.deleteByOidAndUri(oid, uri))
    }

}

