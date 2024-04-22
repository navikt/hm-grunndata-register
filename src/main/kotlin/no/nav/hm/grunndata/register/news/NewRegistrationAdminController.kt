package no.nav.hm.grunndata.register.news

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.jpa.criteria.impl.LiteralExpression
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.data.runtime.criteria.where
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.NewsStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.agreement.AgreementRegistration
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory
import java.util.*

@Secured(Roles.ROLE_ADMIN)
@Controller(NewRegistrationAdminController.ADMIN_API_V1_NEWS)
class NewRegistrationAdminController(private val newsRegistrationService: NewsRegistrationService) {
    companion object {
        private val LOG = LoggerFactory.getLogger(NewRegistrationAdminController::class.java)
        const val ADMIN_API_V1_NEWS = "/admin/api/v1/news"
    }

    @Get("/{?params*}")
    suspend fun getNews(@QueryValue params: Map<String, String>?, pageable: Pageable): Page<NewsRegistrationDTO> {
        return newsRegistrationService.findAll(buildSpec(params), pageable)
    }

    private fun buildSpec(params: Map<String, String>?): PredicateSpecification<NewsRegistration>? =  params?.let {
        where {
            if (params.contains("status")) root[NewsRegistration::status] eq params["status"]
            if (params.contains("draftStatus")) root[NewsRegistration::draftStatus] eq params["draftStatus"]
            if (params.contains("createdByUser")) root[NewsRegistration::createdByUser] eq params["createdByUser"]
        }.and { root, criteriaBuilder ->
            if (params.contains("title")) {
                criteriaBuilder.like(root[NewsRegistration::title], LiteralExpression("%${params["title"]}%"))
            } else {
                null
            }
        }
    }


    @Post("/")
    suspend fun createNews(@Body news: NewsRegistrationDTO): NewsRegistrationDTO {
        LOG.info("Creating news: ${news.title}")
        newsRegistrationService.findById(news.id)?.let {
            throw BadRequestException("News with id ${news.id} already exists")
        }
        return newsRegistrationService.saveAndCreateEventIfNotDraft(news, isUpdate = false)
    }

    @Post("/draft")
    suspend fun createNewsDraft(authentication: Authentication): NewsRegistrationDTO {
        LOG.info("Creating news draft")
        return newsRegistrationService.saveAndCreateEventIfNotDraft(
            NewsRegistrationDTO(
                title = "Draft",
                text = "Draft",
                draftStatus = DraftStatus.DRAFT,
                createdBy = REGISTER,
                updatedBy = REGISTER,
                author = "Admin",
                createdByUser = authentication.name,
                updatedByUser = authentication.name
            ),
            isUpdate = false
        )
    }

    @Put("/{id}")
    suspend fun updateNews(@Body news: NewsRegistrationDTO, @PathVariable id: UUID, authentication: Authentication): NewsRegistrationDTO {
        LOG.info("Updating news: $id")
        return newsRegistrationService.findById(id)?.let { inDb ->
            newsRegistrationService.saveAndCreateEventIfNotDraft(
                inDb.copy(
                    title = news.title,
                    text = news.text,
                    updatedByUser = authentication.name,
                    status = news.status,
                    expired = news.expired,
                    published = news.published,
                    updated = news.updated
                ),
                isUpdate = true
            )
        } ?: throw BadRequestException("News with id ${news.id} does not exist")
    }

    @Delete("/{id}")
    suspend fun deleteNews(@PathVariable id: UUID, authentication: Authentication) {
        LOG.info("Deleting news: $id")
        newsRegistrationService.findById(id)?.let { inDb ->
            newsRegistrationService.saveAndCreateEventIfNotDraft(
                inDb.copy(status = NewsStatus.DELETED), isUpdate = true)
        } ?: throw BadRequestException("News with id $id does not exist")
    }


}