package no.nav.hm.grunndata.register.media

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.http.multipart.CompletedFileUpload
import no.nav.hm.grunndata.rapid.dto.MediaDTO
import java.util.*

@Client("\${media.upload.url}")
interface MediaUploadClient {

    @Post(
        value = "/{oid}",
        produces = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA]
    )
    suspend fun uploadFile(oid: UUID, @Body file: MultipartBody): HttpResponse<MediaDTO>

}
