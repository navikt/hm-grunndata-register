package no.nav.hm.grunndata.register.agreement

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.data.runtime.criteria.where
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType.*
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.api.BadRequestException
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory
import java.util.*

@Secured(Roles.ROLE_ADMIN)
@Controller(AgreementRegistrationAdminApiController.API_V1_ADMIN_AGREEMENT_REGISTRATIONS)
class AgreementRegistrationAdminApiController(private val agreementRegistrationService: AgreementRegistrationService) {

    companion object {
        const val API_V1_ADMIN_AGREEMENT_REGISTRATIONS = "/admin/api/v1/agreement/registrations"
        private val LOG = LoggerFactory.getLogger(AgreementRegistrationAdminApiController::class.java)
    }


    @Get("/{?params*}")
    suspend fun findAgreements(@QueryValue params: HashMap<String,String>?,
                             pageable: Pageable): Page<AgreementRegistrationDTO> =
        agreementRegistrationService.findAll(buildCriteriaSpec(params), pageable)


    private fun buildCriteriaSpec(params: HashMap<String, String>?): PredicateSpecification<AgreementRegistration>?
    = params?.let {
        where {
            if (params.contains("reference")) root[AgreementRegistration::reference] eq params["reference"]
            if (params.contains("title")) criteriaBuilder.like(root[AgreementRegistration::title], params["title"])
            if (params.contains("draftStatus")) root[AgreementRegistration::draftStatus] eq DraftStatus.valueOf(params["draftStatus"]!!)
            if (params.contains("createdByUser")) root[AgreementRegistration::createdByUser] eq params["createdByUser"]
            if (params.contains("updatedByUser")) root[AgreementRegistration::updatedByUser] eq params["updatedByUser"]
        }
    }



    @Get("/{id}")
    suspend fun getAgreementById(id: UUID): HttpResponse<AgreementRegistrationDTO> =
        agreementRegistrationService.findById(id)
            ?.let {
                HttpResponse.ok(it) }
            ?: HttpResponse.notFound()

    @Post("/")
    suspend fun createAgreement(@Body registrationDTO: AgreementRegistrationDTO, authentication: Authentication): HttpResponse<AgreementRegistrationDTO> =
        agreementRegistrationService.findById(registrationDTO.id)?.let {
                throw BadRequestException("agreement ${registrationDTO.id} already exists")
            } ?: run {
                val dto = agreementRegistrationService.saveAndPushToRapid(registrationDTO
                    .copy(createdByUser = authentication.name, updatedByUser = authentication.name), isUpdate = false)
                HttpResponse.created(dto)
            }

    @Put("/{id}")
    suspend fun updateAgreement(@Body registrationDTO: AgreementRegistrationDTO, @PathVariable id: UUID, authentication: Authentication):
            HttpResponse<AgreementRegistrationDTO> =
        agreementRegistrationService.findById(id)
                ?.let { inDb ->
                    val updated = registrationDTO.copy(id = inDb.id, created = inDb.created, createdByUser = inDb.createdByUser,
                        updatedByUser = authentication.name, updatedBy = REGISTER, createdBy = inDb.createdBy)
                    val dto = agreementRegistrationService.saveAndPushToRapid(updated, isUpdate = true)
                    HttpResponse.ok(dto) }
                ?: run {
                    throw BadRequestException("${registrationDTO.id} does not exists")}

}


