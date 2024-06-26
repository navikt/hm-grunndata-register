package no.nav.hm.grunndata.register.agreement

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.jpa.criteria.impl.LiteralExpression
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.data.runtime.criteria.where
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.rapid.dto.AgreementPost
import no.nav.hm.grunndata.rapid.dto.AgreementStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

@Secured(Roles.ROLE_ADMIN)
@Controller(AgreementRegistrationAdminApiController.API_V1_ADMIN_AGREEMENT_REGISTRATIONS)
@Tag(name = "Admin Agreement")
class AgreementRegistrationAdminApiController(private val agreementRegistrationService: AgreementRegistrationService) {
    companion object {
        const val API_V1_ADMIN_AGREEMENT_REGISTRATIONS = "/admin/api/v1/agreement/registrations"
        private val LOG = LoggerFactory.getLogger(AgreementRegistrationAdminApiController::class.java)
    }

    @Get("/{?params*}")
    suspend fun findAgreements(
        @QueryValue params: HashMap<String, String>?,
        pageable: Pageable,
    ): Page<AgreementBasicInformationDto> = agreementRegistrationService.findAll(buildCriteriaSpec(params), pageable)

    private fun buildCriteriaSpec(params: HashMap<String, String>?): PredicateSpecification<AgreementRegistration>? =
        params?.let {
            where {
                if (params.contains("reference")) root[AgreementRegistration::reference] eq params["reference"]
                if (params.contains("draftStatus")) {
                    root[AgreementRegistration::draftStatus] eq
                        DraftStatus.valueOf(
                            params["draftStatus"]!!,
                        )
                }
                if (params.contains("agreementStatus")) root[AgreementRegistration::agreementStatus] eq params["agreementStatus"]
                if (params.contains(
                        "excludedAgreementStatus",
                    )
                ) {
                    root[AgreementRegistration::agreementStatus] ne params["excludedAgreementStatus"]
                }
                if (params.contains("createdByUser")) root[AgreementRegistration::createdByUser] eq params["createdByUser"]
                if (params.contains("updatedByUser")) root[AgreementRegistration::updatedByUser] eq params["updatedByUser"]
                if (params.contains("filter")) {
                    if (params["filter"] == "ACTIVE") {
                        root[AgreementRegistration::agreementStatus] eq AgreementStatus.ACTIVE
                    } else if (params["filter"] == "EXPIRED") {
                        root[AgreementRegistration::expired] lessThan LocalDateTime.now()
                    } else if (params["filter"] == "FUTURE") {
                        root[AgreementRegistration::published] greaterThan LocalDateTime.now()
                    }
                }
            }.and { root, criteriaBuilder ->
                if (params.contains("title")) {
                    criteriaBuilder.like(root[AgreementRegistration::title], LiteralExpression("%${params["title"]}%"))
                } else {
                    null
                }
            }
        }

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

data class AgreementDraftWithDTO(
    val title: String,
    val reference: String,
    val published: LocalDateTime,
    val expired: LocalDateTime,
)
