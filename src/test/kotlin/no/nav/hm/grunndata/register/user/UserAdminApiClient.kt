package no.nav.hm.grunndata.register.user

import io.micronaut.data.model.Page
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.http.client.annotation.Client
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
                 @QueryValue("sort") sort: String? = null): Page<UserDTO>

    @Get("/supplierId/{supplierId}")
    fun getUsersBySupplierId(@CookieValue("JWT") jwt: String, supplierId: UUID): List<UserDTO>


    @Get("/email/{email}")
    fun getUserByEmail(@CookieValue("JWT") jwt: String, email:String) : HttpResponse<UserDTO>

    @Put("/{id}")
    fun updateUser(@CookieValue("JWT") jwt: String, id: UUID, @Body userDTO: UserDTO): HttpResponse<UserDTO>

    @Get("/{id}")
    fun getUser(@CookieValue("JWT") jwt: String, id:UUID) : HttpResponse<UserDTO>
}
