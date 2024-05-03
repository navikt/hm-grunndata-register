package no.nav.hm.grunndata.register.media

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationService
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.media.MediaAdminController.Companion.API_V1_ADMIN_UPLOAD_MEDIA
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.series.SeriesRegistrationService
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import java.util.*

@Secured(Roles.ROLE_ADMIN)
@Controller(API_V1_ADMIN_UPLOAD_MEDIA)
@Tag(name="Admin Media")
class MediaAdminController(private val mediaUploadService: MediaUploadService,
                           private val productRegistrationService: ProductRegistrationService,
                           private val seriesRegistrationService: SeriesRegistrationService,
                           private val agreementRegistrationService: AgreementRegistrationService) {

    companion object {
        const val API_V1_ADMIN_UPLOAD_MEDIA = "/admin/api/v1/media"
        private val LOG = LoggerFactory.getLogger(MediaAdminController::class.java)
    }

    @Post(
        value = "/{type}/files/{oid}",
        consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA],
        produces = [io.micronaut.http.MediaType.APPLICATION_JSON]
    )
    suspend fun uploadProductFiles(@Parameter(example = "series, product or agreement") type: String="product", oid: UUID,
                                   files: Publisher<CompletedFileUpload>,
                                   authentication: Authentication): HttpResponse<List<MediaDTO>>  {
        if ("product" == type && productRegistrationService.findById(oid) != null) {
            return HttpResponse.created(files.asFlow().map {mediaUploadService.uploadMedia(it, oid, ObjectType.PRODUCT) }.toList())
        } else if ("series" == type && seriesRegistrationService.findById(oid) != null) {
            return HttpResponse.created(files.asFlow().map {mediaUploadService.uploadMedia(it, oid, ObjectType.SERIES) }.toList())
        } else if ("agreement" == type && agreementRegistrationService.findById(oid) != null) {
            return HttpResponse.created(files.asFlow().map {mediaUploadService.uploadMedia(it, oid, ObjectType.AGREEMENT) }.toList())
        }
        throw BadRequestException("Unknown type $type or oid: $oid")
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

