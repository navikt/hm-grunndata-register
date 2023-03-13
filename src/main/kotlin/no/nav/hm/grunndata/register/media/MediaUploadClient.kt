package no.nav.hm.grunndata.register.media

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.multipart.CompletedFileUpload
import no.nav.hm.grunndata.rapid.dto.MediaDTO
import java.util.*

@Client("\${MEDIA.UPLOAD.URL}")
interface MediaUploadClient {

    @Post(
        value = "/{oid}",
        consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA],
        produces = [io.micronaut.http.MediaType.TEXT_PLAIN]
    )
    suspend fun uploadFile(oid: UUID, file: CompletedFileUpload): HttpResponse<MediaDTO>

}
