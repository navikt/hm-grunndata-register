package no.nav.hm.grunndata.register.media

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.multipart.MultipartBody
import no.nav.hm.grunndata.rapid.dto.MediaDTO
import java.util.*

@Client("\${media.upload.url}")
interface MediaUploadClient {

    @Post(
        value = "/file/{oid}",
        produces = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA]
    )
    suspend fun uploadFile(oid: UUID, @Body file: MultipartBody): MediaDTO

    @Post(
        value = "/files/{oid}",
        produces = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA]
    )
    suspend fun uploadFiles(oid: UUID, @Body files: List<MultipartBody>): List<MediaDTO>

}