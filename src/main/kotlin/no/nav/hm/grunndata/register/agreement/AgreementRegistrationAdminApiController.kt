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
import no.nav.hm.grunndata.register.product.REGISTER
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory
import java.util.*

@Secured(Roles.ROLE_ADMIN)
@Controller(AgreementRegistrationAdminApiController.API_V1_ADMIN_AGREEMENT_REGISTRATIONS)
class AgreementRegistrationAdminApiController(private val agreementRegistrationRepository: AgreementRegistrationRepository) {

    companion object {
        const val API_V1_ADMIN_AGREEMENT_REGISTRATIONS = "/api/v1/admin/agreement/registrations"
        private val LOG = LoggerFactory.getLogger(AgreementRegistrationAdminApiController::class.java)
    }


    @Get("/{?params*}")
    suspend fun findAgreements(@QueryValue params: HashMap<String,String>?,
                             pageable: Pageable): Page<AgreementRegistrationDTO> =
        agreementRegistrationRepository.findAll(buildCriteriaSpec(params), pageable).map { it.toDTO() }


    private fun buildCriteriaSpec(params: HashMap<String, String>?): PredicateSpecification<AgreementRegistration>?
    = params?.let {
        where {
            if (params.contains("reference")) root[AgreementRegistration::reference] eq params["reference"]
            if (params.contains("draftStatus")) root[AgreementRegistration::draftStatus] eq DraftStatus.valueOf(params["draftStatus"]!!)
            if (params.contains("createdByUser")) root[AgreementRegistration::createdByUser] eq params["createdByUser"]
            if (params.contains("updatedByUser")) root[AgreementRegistration::updatedByUser] eq params["updatedByUser"]
        }
    }



    @Get("/{id}")
    suspend fun getAgreementById(id: UUID): HttpResponse<AgreementRegistrationDTO> =
        agreementRegistrationRepository.findById(id)
            ?.let {
                HttpResponse.ok(it.toDTO()) }
            ?: HttpResponse.notFound()

    @Post("/")
    suspend fun createAgreement(@Body registrationDTO: AgreementRegistrationDTO, authentication: Authentication): HttpResponse<AgreementRegistrationDTO> =
            agreementRegistrationRepository.findById(registrationDTO.id)?.let {
                HttpResponse.badRequest()
            } ?: run {
                HttpResponse.created(agreementRegistrationRepository.save(registrationDTO
                    .copy(createdByUser = authentication.name, updatedByUser = authentication.name)
                    .toEntity()).toDTO())
            }

    @Put("/{id}")
    suspend fun updateAgreement(@Body registrationDTO: AgreementRegistrationDTO, @PathVariable id: UUID, authentication: Authentication):
            HttpResponse<AgreementRegistrationDTO> =
        agreementRegistrationRepository.findById(id)
                ?.let { inDb ->
                    val updated = registrationDTO.copy(id = inDb.id, created = inDb.created, createdByUser = inDb.createdByUser,
                        updatedByUser = authentication.name, updatedBy = REGISTER, createdBy = inDb.createdBy)
                    HttpResponse.ok(agreementRegistrationRepository.update(updated.toEntity()).toDTO()) }
                ?: run {
                    HttpResponse.badRequest() }

}


