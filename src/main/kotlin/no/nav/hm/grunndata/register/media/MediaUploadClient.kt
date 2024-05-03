package no.nav.hm.grunndata.register.media

import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.multipart.MultipartBody
import java.util.*

@Client("\${media.upload.url}/api/v1/upload/media/register")
interface MediaUploadClient {

    @Post(
        value = "/file/{oid}",
        produces = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA]
    )
    suspend fun uploadFile(@PathVariable oid: UUID, @QueryValue objectType: ObjectType,  @Body file: MultipartBody): MediaDTO

    @Get(value = "/oid/{oid}")
    suspend fun getMediaList(oid:UUID): List<MediaDTO>

    @Delete(value= "/{oid}/{uri}")
    suspend fun deleteByOidAndUri(oid: UUID, uri: String): MediaDTO?


}
