package no.nav.hm.grunndata.register.news

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.jpa.criteria.impl.expression.LiteralExpression
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
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
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.NewsStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import no.nav.hm.grunndata.register.runtime.where

@Secured(Roles.ROLE_ADMIN)
@Controller(NewRegistrationAdminController.ADMIN_API_V1_NEWS)
@Tag(name = "Admin News")
class NewRegistrationAdminController(private val newsRegistrationService: NewsRegistrationService) {
    companion object {
        private val LOG = LoggerFactory.getLogger(NewRegistrationAdminController::class.java)
        const val ADMIN_API_V1_NEWS = "/admin/api/v1/news"
    }

    @Get("/")
    suspend fun getNews(@RequestBean criteria: NewsRegistrationCriteria, pageable: Pageable): Page<NewsRegistrationDTO> {
        return newsRegistrationService.findAll(buildSpec(criteria), pageable)
    }

    private fun buildSpec(criteria: NewsRegistrationCriteria): PredicateSpecification<NewsRegistration>? =
        if (criteria.isNotEmpty()) {
        where {
            if (criteria.status.isNotEmpty()) { root[NewsRegistration::status] inList criteria.status }
            criteria.draftStatus?.let { root[NewsRegistration::draftStatus] eq it }
            criteria.createdByUser?.let { root[NewsRegistration::createdByUser] eq it }
            criteria.title?.let { root[NewsRegistration::title] like LiteralExpression("%$it%") }
        }
    } else null

    @Post("/")
    suspend fun createNews(@Body news: CreateUpdateNewsDTO, authentication: Authentication): NewsRegistrationDTO {
        LOG.info("Creating news: ${news.title}")

        return newsRegistrationService.saveAndCreateEventIfNotDraft(
            NewsRegistrationDTO(
                title = news.title,
                text = news.text,
                published = news.published,
                expired = news.expired,
                draftStatus = DraftStatus.DONE,
                createdBy = REGISTER,
                updatedBy = REGISTER,
                author = "Admin",
                createdByUser = authentication.name,
                updatedByUser = authentication.name
            ),
            isUpdate = false
        )
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

    @Put("/publish/{id}")
    suspend fun publishNews(@PathVariable id: UUID, authentication: Authentication): NewsRegistrationDTO {
        LOG.info("Publishing news: $id")
        return newsRegistrationService.findById(id)?.let { inDb ->
            newsRegistrationService.saveAndCreateEventIfNotDraft(
                inDb.copy(
                    published = LocalDateTime.now(),
                    updated = LocalDateTime.now(),
                    updatedByUser = authentication.name,
                ),
                isUpdate = true
            )
        } ?: throw BadRequestException("News with id $id does not exist")
    }

    @Put("/unpublish/{id}")
    suspend fun unpublishNews(@PathVariable id: UUID, authentication: Authentication): NewsRegistrationDTO {
        LOG.info("unpublishing news: $id")
        return newsRegistrationService.findById(id)?.let { inDb ->
            newsRegistrationService.saveAndCreateEventIfNotDraft(
                inDb.copy(
                    expired = LocalDateTime.now().minusDays(1),
                    updated = LocalDateTime.now(),
                    updatedByUser = authentication.name,
                ),
                isUpdate = true
            )
        } ?: throw BadRequestException("News with id $id does not exist")
    }

    @Put("/{id}")
    suspend fun updateNews(
        @Body news: CreateUpdateNewsDTO,
        @PathVariable id: UUID,
        authentication: Authentication
    ): NewsRegistrationDTO {
        LOG.info("Updating news: $id")
        return newsRegistrationService.findById(id)?.let { inDb ->
            newsRegistrationService.saveAndCreateEventIfNotDraft(
                inDb.copy(
                    title = news.title,
                    text = news.text,
                    expired = news.expired,
                    published = news.published,
                    updated = LocalDateTime.now(),
                    updatedByUser = authentication.name,
                ),
                isUpdate = true
            )
        } ?: throw BadRequestException("News with id $id does not exist")
    }

    @Delete("/{id}")
    suspend fun deleteNews(@PathVariable id: UUID, authentication: Authentication) {
        LOG.info("Deleting news: $id")
        newsRegistrationService.findById(id)?.let { inDb ->
            newsRegistrationService.saveAndCreateEventIfNotDraft(
                inDb.copy(status = NewsStatus.DELETED, expired = LocalDateTime.now()), isUpdate = true
            )
        } ?: throw BadRequestException("News with id $id does not exist")
    }

    @Introspected
    data class NewsRegistrationCriteria (
        val status: List<NewsStatus> = emptyList(),
        val draftStatus: DraftStatus? = null,
        val createdByUser: String? = null,
        val title: String? = null
    ) {
        fun isNotEmpty() = status.isNotEmpty() || draftStatus != null || createdByUser != null || title != null
    }
}

data class CreateUpdateNewsDTO(
    val title: String,
    val text: String,
    val published: LocalDateTime,
    val expired: LocalDateTime
)