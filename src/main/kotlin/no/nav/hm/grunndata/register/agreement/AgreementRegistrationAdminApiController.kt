package no.nav.hm.grunndata.register.agreement

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.jpa.criteria.impl.expression.LiteralExpression
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.RequestBean
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import java.time.LocalDateTime
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.AgreementPost
import no.nav.hm.grunndata.rapid.dto.AgreementStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.runtime.where
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory

@Secured(Roles.ROLE_ADMIN)
@Controller(AgreementRegistrationAdminApiController.API_V1_ADMIN_AGREEMENT_REGISTRATIONS)
@Tag(name = "Admin Agreement")
class AgreementRegistrationAdminApiController(private val agreementRegistrationService: AgreementRegistrationService) {
    companion object {
        const val API_V1_ADMIN_AGREEMENT_REGISTRATIONS = "/admin/api/v1/agreement/registrations"
        private val LOG = LoggerFactory.getLogger(AgreementRegistrationAdminApiController::class.java)
    }

    @Get("/")
    suspend fun findAgreements(
        @RequestBean criteria: AgreementAdminCriteria,
        pageable: Pageable
    ): Page<AgreementBasicInformationDto> {
        return agreementRegistrationService.findAll(buildCriteriaSpec(criteria), pageable)
    }

    private fun buildCriteriaSpec(criteria: AgreementAdminCriteria): PredicateSpecification<AgreementRegistration>? =
        if (criteria.isNotEmpty()) {
            where {
                criteria.reference?.let { root[AgreementRegistration::reference] eq it }
                criteria.draftStatus?.let { root[AgreementRegistration::draftStatus] eq it }
                criteria.agreementStatus?.let { root[AgreementRegistration::agreementStatus] eq it }
                criteria.excludedAgreementStatus?.let { root[AgreementRegistration::agreementStatus] ne it }
                criteria.filter?.let {
                    if (criteria.filter == "ACTIVE") {
                        root[AgreementRegistration::agreementStatus] eq AgreementStatus.ACTIVE
                    } else if (criteria.filter == "EXPIRED") {
                        root[AgreementRegistration::expired] lessThan LocalDateTime.now()
                    } else if (criteria.filter == "FUTURE") {
                        root[AgreementRegistration::published] greaterThan LocalDateTime.now()
                    }
                }
                criteria.createdByUser?.let { root[AgreementRegistration::createdByUser] eq it }
                criteria.updatedByUser?.let { root[AgreementRegistration::updatedByUser] eq it }
                criteria.title?.let { root[AgreementRegistration::title] like LiteralExpression("%${it}%") }
            }
        } else null

    @Get("/{id}")
    suspend fun getAgreementById(id: UUID): HttpResponse<AgreementRegistrationDTO> =
        agreementRegistrationService.findById(id)
            ?.let {
                HttpResponse.ok(it)
            }
            ?: HttpResponse.notFound()

    @Get("/{id}/delkontrakt/{delkontraktId}")
    suspend fun getDelkontraktById(
        id: UUID,
        delkontraktId: String,
    ): HttpResponse<AgreementPost> =
        agreementRegistrationService.findById(id)
            ?.let {
                it.agreementData.posts.find { post -> post.identifier == delkontraktId }
                    ?.let { post -> HttpResponse.ok(post) }
                    ?: HttpResponse.notFound()
            }
            ?: HttpResponse.notFound()

    @Delete("/{id}/delkontrakt/{delkontraktId}")
    suspend fun deleteDelkontraktById(
        id: UUID,
        delkontraktId: String,
    ) = agreementRegistrationService.findById(id)
        ?.let { inDb ->
            val updated =
                inDb.copy(
                    agreementData =
                    inDb.agreementData.copy(
                        posts = inDb.agreementData.posts.filter { post -> post.identifier != delkontraktId },
                    ),
                )
            val dto = agreementRegistrationService.saveAndCreateEventIfNotDraft(updated, isUpdate = true)
            HttpResponse.ok(dto)
        }
        ?: HttpResponse.notFound()

    @Post("/")
    suspend fun createAgreement(
        @Body registrationDTO: AgreementRegistrationDTO,
        authentication: Authentication,
    ): HttpResponse<AgreementRegistrationDTO> =
        agreementRegistrationService.findById(registrationDTO.id)?.let {
            throw BadRequestException("agreement ${registrationDTO.id} already exists")
        } ?: run {
            val dto =
                agreementRegistrationService.saveAndCreateEventIfNotDraft(
                    registrationDTO
                        .copy(createdByUser = authentication.name, updatedByUser = authentication.name),
                    isUpdate = false,
                )
            HttpResponse.created(dto)
        }

    @Post("/draft/reference/{reference}")
    suspend fun draftAgreement(
        reference: String,
        authentication: Authentication,
    ): HttpResponse<AgreementRegistrationDTO> =
        agreementRegistrationService.findByReference(reference)?.let {
            throw BadRequestException("agreement reference $reference already exists")
        } ?: run {
            val draft =
                AgreementRegistrationDTO(
                    id = UUID.randomUUID(),
                    draftStatus = DraftStatus.DRAFT,
                    agreementStatus = AgreementStatus.INACTIVE,
                    title = "Fyll ut title",
                    reference = reference,
                    expired = LocalDateTime.now().plusYears(3),
                    createdByUser = authentication.name,
                    updatedByUser = authentication.name,
                    agreementData =
                    AgreementData(
                        resume = "kort beskrivelse",
                        text = "rammeavtale tekst her",
                        identifier = UUID.randomUUID().toString(),
                        attachments = emptyList(),
                        posts = emptyList(),
                    ),
                )
            val dto = agreementRegistrationService.saveAndCreateEventIfNotDraft(draft, isUpdate = false)
            HttpResponse.created(dto)
        }

    @Post("/draft/reference")
    suspend fun draftAgreementWith(
        @Body draftWith: AgreementDraftWithDTO,
        authentication: Authentication,
    ): HttpResponse<AgreementRegistrationDTO> =
        agreementRegistrationService.findByReference(draftWith.reference)?.let {
            throw BadRequestException("agreement reference ${draftWith.reference} already exists")
        } ?: run {
            val draft =
                AgreementRegistrationDTO(
                    id = UUID.randomUUID(),
                    draftStatus = DraftStatus.DRAFT,
                    agreementStatus = AgreementStatus.INACTIVE,
                    title = draftWith.title,
                    reference = draftWith.reference,
                    expired = draftWith.expired,
                    published = draftWith.published,
                    createdByUser = authentication.name,
                    updatedByUser = authentication.name,
                    agreementData =
                    AgreementData(
                        resume = "kort beskrivelse",
                        text = "rammeavtaletekst her",
                        identifier = UUID.randomUUID().toString(),
                        attachments = emptyList(),
                        posts = emptyList(),
                    ),
                )
            val dto = agreementRegistrationService.saveAndCreateEventIfNotDraft(draft, isUpdate = false)
            HttpResponse.created(dto)
        }

    @Post("/draft/from/{id}/reference/{reference}")
    suspend fun createAgreementFromAnother(
        id: UUID,
        reference: String,
        authentication: Authentication,
    ): HttpResponse<AgreementRegistrationDTO> =
        agreementRegistrationService.findById(id)?.let {
            HttpResponse.created(
                agreementRegistrationService.save(
                    it.copy(
                        id = UUID.randomUUID(),
                        reference = reference,
                        draftStatus = DraftStatus.DRAFT,
                        agreementStatus = AgreementStatus.INACTIVE,
                        created = LocalDateTime.now(),
                        updated = LocalDateTime.now(),
                        expired = LocalDateTime.now().plusYears(5),
                        createdByUser = authentication.name,
                        updatedByUser = authentication.name,
                        createdBy = REGISTER,
                        updatedBy = REGISTER,
                    ),
                ),
            )
        } ?: throw BadRequestException("wrong $id")

    @Put("/{id}")
    suspend fun updateAgreement(
        @Body registrationDTO: AgreementRegistrationDTO,
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<AgreementRegistrationDTO> =
        agreementRegistrationService.findById(id)
            ?.let { inDb ->
                val updated =
                    registrationDTO.copy(
                        id = inDb.id,
                        draftStatus = if (inDb.draftStatus == DraftStatus.DONE) DraftStatus.DONE else registrationDTO.draftStatus,
                        created = inDb.created,
                        createdByUser = inDb.createdByUser,
                        updatedByUser = authentication.name,
                        updatedBy = REGISTER,
                        createdBy = inDb.createdBy,
                        updated = LocalDateTime.now()
                    )
                val dto = agreementRegistrationService.saveAndCreateEventIfNotDraft(updated, isUpdate = true)
                HttpResponse.ok(dto)
            }
            ?: run {
                throw BadRequestException("${registrationDTO.id} does not exists")
            }

    @Delete("/{id}")
    suspend fun deleteAgreement(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<AgreementRegistrationDTO> =
        agreementRegistrationService.findById(id)
            ?.let {
                val dto =
                    agreementRegistrationService.saveAndCreateEventIfNotDraft(
                        it.copy(
                            agreementStatus = AgreementStatus.DELETED,
                            updatedByUser = authentication.name,
                            updatedBy = REGISTER,
                            updated = LocalDateTime.now(),
                            expired = LocalDateTime.now(),
                        ),
                        isUpdate = true,
                    )
                HttpResponse.ok(dto)
            }
            ?: HttpResponse.notFound()

}

@Introspected
data class AgreementAdminCriteria(
    val title: String?,
    val reference: String?,
    val draftStatus: DraftStatus?,
    val agreementStatus: AgreementStatus?,
    val excludedAgreementStatus: AgreementStatus?,
    val createdByUser: String?,
    val updatedByUser: String?,
    val filter: String?,
) {
    fun isNotEmpty() = title != null || reference != null || draftStatus != null || agreementStatus != null ||
            excludedAgreementStatus != null || createdByUser != null || updatedByUser != null || filter != null
}

data class AgreementDraftWithDTO(
    val title: String,
    val reference: String,
    val published: LocalDateTime,
    val expired: LocalDateTime,
)
