package no.nav.hm.grunndata.register.agreement

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.data.runtime.criteria.where
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.RequestBean
import io.micronaut.security.annotation.Secured
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory

@Secured(Roles.ROLE_SUPPLIER)
@Controller(AgreementRegistrationApiController.API_V1_AGREEMENT_REGISTRATIONS)
@Tag(name="Vendor Agreement")
class AgreementRegistrationApiController(private val agreementRegistrationService: AgreementRegistrationService) {
    companion object {
        const val API_V1_AGREEMENT_REGISTRATIONS = "/vendor/api/v1/agreement/registrations"
        private val LOG = LoggerFactory.getLogger(AgreementRegistrationApiController::class.java)
    }

    @Get("/")
    suspend fun findAgreements(
        @RequestBean agreementCriteria: AgreementCriteria,
        pageable: Pageable,
    ): Page<AgreementBasicInformationDto> = agreementRegistrationService.findAll(buildCriteriaSpec(agreementCriteria), pageable)

    private fun buildCriteriaSpec(criteria: AgreementCriteria): PredicateSpecification<AgreementRegistration>? =
        if (criteria.isNotEmpty()) {
            where {
                criteria.reference?.let { root[AgreementRegistration::reference] eq it }
            }.and { root, criteriaBuilder ->
                if (criteria.title != null) {
                    criteriaBuilder.like(root[AgreementRegistration::title], criteriaBuilder.literal("%${criteria.title}%"))
                } else null
            }
        } else null

    @Introspected
    data class AgreementCriteria(
        val reference: String?,
        val title: String?,
    ) {
        fun isNotEmpty(): Boolean = reference != null || title != null
    }
}
