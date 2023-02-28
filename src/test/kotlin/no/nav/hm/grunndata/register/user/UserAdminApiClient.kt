package no.nav.hm.grunndata.register.user

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.http.client.annotation.Client
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import java.util.*

@Client(UserAdminApiController.API_V1_ADMIN_USER_REGISTRATIONS)
interface UserAdminApiClient {

    @Post("/")
    fun createUser(@CookieValue("JWT") jwt: String, @Body dto: UserRegistrationDTO): HttpResponse<UserDTO>

    @Get("/")
    fun getUsers(@CookieValue("JWT") jwt: String,
                 @QueryValue email: String? = null,
                 @QueryValue name: String? = null,
                 @QueryValue supplierId: UUID? = null,
                 @QueryValue("size") size: Int? = null,
                 @QueryValue("page") page: Int?=null,
                 @QueryValue("sort") sort: String? = null)

}
