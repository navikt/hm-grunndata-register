package no.nav.hm.grunndata.register.agreement

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.data.runtime.criteria.where
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.security.annotation.Secured
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory
import java.util.HashMap

@Secured(Roles.ROLE_SUPPLIER)
@Controller(AgreementRegistrationApiController.API_V1_AGREEMENT_REGISTRATIONS)
class AgreementRegistrationApiController(private val agreementRegistrationService: AgreementRegistrationService) {

    companion object {
        const val API_V1_AGREEMENT_REGISTRATIONS = "/api/v1/agreement/registrations"
        private val LOG = LoggerFactory.getLogger(AgreementRegistrationApiController::class.java)
    }

    @Get("/{?params*}")
    suspend fun findAgreements(@QueryValue params: HashMap<String, String>?,
                               pageable: Pageable
    ): Page<AgreementRegistrationDTO> =
        agreementRegistrationService.findAll(buildCriteriaSpec(params), pageable)


    private fun buildCriteriaSpec(params: HashMap<String, String>?): PredicateSpecification<AgreementRegistration>?
            = params?.let {
        where {
            if (params.contains("reference")) root[AgreementRegistration::reference] eq params["reference"]
            if (params.contains("title")) criteriaBuilder.like(root[AgreementRegistration::title], params["title"])
          }
    }

}