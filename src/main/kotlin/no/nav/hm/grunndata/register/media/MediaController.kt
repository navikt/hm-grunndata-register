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
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.media.MediaController.Companion.API_V1_UPLOAD_PRODUCT_MEDIA
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.supplierId
import no.nav.hm.grunndata.register.series.SeriesRegistrationService
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import java.util.*

@Secured(Roles.ROLE_SUPPLIER)
@Controller(API_V1_UPLOAD_PRODUCT_MEDIA)
class MediaController(private val mediaUploadService: MediaUploadService,
                      private val seriesRegistrationService: SeriesRegistrationService,
                      private val productRegistrationService: ProductRegistrationService) {

    companion object {
        const val API_V1_UPLOAD_PRODUCT_MEDIA = "/vendor/api/v1/media"
        private val LOG = LoggerFactory.getLogger(MediaAdminController::class.java)
    }


    @Get("/{oid}")
    @Deprecated("Use getMediaList(type: String, oid: UUID) instead")
    suspend fun getMediaList(oid:UUID, authentication: Authentication): HttpResponse<List<MediaDTO>> {
        if (productRegistrationService.findByIdAndSupplierId(oid, authentication.supplierId()) != null) {
            return HttpResponse.ok(mediaUploadService.getMediaList(oid))
        }
        throw BadRequestException("Wrong id?")
    }

    @Get("/{type}/{oid}")
    suspend fun getMediaList(type: String="product", oid:UUID, authentication: Authentication): HttpResponse<List<MediaDTO>> {
        if ("product" == type && productRegistrationService.findByIdAndSupplierId(oid, authentication.supplierId())!=null) {
            return HttpResponse.ok(mediaUploadService.getMediaList(oid))
        } else if ("series" == type && seriesRegistrationService.findByIdAndSupplierId(oid, authentication.supplierId())!=null) {
            return HttpResponse.ok(mediaUploadService.getMediaList(oid))
        }
        throw BadRequestException("Wrong type $type or id: $oid")
    }

    @Post(
        value = "/{type}/files/{oid}",
        consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA],
        produces = [io.micronaut.http.MediaType.APPLICATION_JSON]
    )
    suspend fun uploadFiles(type: String="product", oid: UUID,
                            files: Publisher<CompletedFileUpload>,
                            authentication: Authentication): HttpResponse<List<MediaDTO>>  {
        LOG.info("supplier: ${authentication.supplierId()} uploading files for object $oid type: $type")
        if ("product" == type && oidExists(oid, authentication.supplierId())) {
            return HttpResponse.created(files.asFlow().map {mediaUploadService.uploadMedia(it, oid, ObjectType.PRODUCT) }.toList())
        } else if ("series" == type && seriesExists(oid, authentication.supplierId())) {
            return HttpResponse.created(files.asFlow().map {mediaUploadService.uploadMedia(it, oid, ObjectType.SERIES) }.toList())
        }
        throw BadRequestException("Not found for $type and  id: $oid")
    }


    @Delete("/{oid}/{uri}")
    @Deprecated("Use deleteFiles(type: String, oid: UUID, uri: String) instead")
    suspend fun deleteFile(oid: UUID, uri: String, authentication: Authentication): HttpResponse<MediaDTO> {
        if (productRegistrationService.findByIdAndSupplierId(oid, authentication.supplierId())!=null) {
            return HttpResponse.ok(mediaUploadService.deleteByOidAndUri(oid, uri))
        }
        throw BadRequestException("Not found for id: $oid uri: $uri")
    }

    @Delete("/{type}/{oid}/{uri}")
    suspend fun deleteFiles(type: String = "product", oid: UUID, uri: String, authentication: Authentication): HttpResponse<MediaDTO> {
        LOG.info("Deleting media file oid: $oid and $uri")
        if ("series" == type && seriesRegistrationService.findByIdAndSupplierId(oid, authentication.supplierId())!=null)
            return HttpResponse.ok(mediaUploadService.deleteByOidAndUri(oid, uri))
        else if ("product" == type && productRegistrationService.findByIdAndSupplierId(oid, authentication.supplierId())!=null)
            return HttpResponse.ok(mediaUploadService.deleteByOidAndUri(oid, uri))
        throw BadRequestException("Not found for $type and id: $oid uri: $uri")
    }

    private suspend fun seriesExists(oid: UUID, supplierId: UUID) = seriesRegistrationService.findByIdAndSupplierId(oid, supplierId) != null

    private suspend fun oidExists(oid: UUID, supplierId: UUID) =
        productRegistrationService.findByIdAndSupplierId(oid, supplierId) != null
}


val CompletedFileUpload.extension: String
    get() = filename.substringAfterLast('.', "")

