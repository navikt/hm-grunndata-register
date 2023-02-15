package no.nav.hm.grunndata.register.agreement

import io.micronaut.data.model.Page
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.client.annotation.Client
import no.nav.hm.grunndata.dto.DraftStatus
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationAdminApiController.Companion.API_V1_ADMIN_AGREEMENT_REGISTRATIONS
import java.util.*

@Client(API_V1_ADMIN_AGREEMENT_REGISTRATIONS)
interface AgreementRegistrationAdminApiClient {

    @Get(uri = "/", consumes = [MediaType.APPLICATION_JSON])
    fun findAgreements(@CookieValue("JWT") jwt: String,
                       @QueryValue draftStatus: DraftStatus? = null,
                       @QueryValue reference: String?=null,
                       @QueryValue createdByUser: String? = null,
                       @QueryValue updatedByUser: String? = null,
                       @QueryValue("size") size: Int? = null,
                       @QueryValue("page") page: Int?=null,
                       @QueryValue("sort") sort: String? = null): Page<AgreementRegistrationDTO>

    @Get(uri = "/{id}", consumes = [MediaType.APPLICATION_JSON])
    fun getAgreementById(@CookieValue("JWT") jwt: String, id: UUID): HttpResponse<AgreementRegistrationDTO>

    @Put(uri= "/{id}", processes = [MediaType.APPLICATION_JSON])
    fun updateAgreement(@CookieValue("JWT") jwt: String, id: UUID,
                        @Body agreementRegistrationDTO: AgreementRegistrationDTO):HttpResponse<AgreementRegistrationDTO>

    @Post(uri= "/", processes = [MediaType.APPLICATION_JSON])
    fun createAgreement(@CookieValue("JWT") jwt: String,
                        @Body agreementRegistrationDTO: AgreementRegistrationDTO):HttpResponse<AgreementRegistrationDTO>

}