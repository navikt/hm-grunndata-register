package no.nav.hm.grunndata.register.news

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.NewsStatus
import no.nav.hm.grunndata.rapid.event.EventName
import java.time.LocalDateTime
import java.util.*

@Singleton
open class NewsRegistrationService(
    private val newsRegistrationRepository: NewsRegistrationRepository,
    private val newsRegistrationEventHandler: NewsRegistrationEventHandler
) {

    @Transactional
    open suspend fun saveAndCreateEventIfNotDraft(dto: NewsRegistrationDTO, isUpdate: Boolean): NewsRegistrationDTO {
        val correctStatus = ensureCorrectStatus(dto)
        val saved = if (isUpdate) update(correctStatus) else save(correctStatus)
        if (saved.draftStatus == DraftStatus.DONE) {
            newsRegistrationEventHandler.queueDTORapidEvent(saved, eventName = EventName.registeredNewsV1)
        }
        return saved
    }

    private fun ensureCorrectStatus(dto: NewsRegistrationDTO): NewsRegistrationDTO {
        val now = LocalDateTime.now()
        return if (dto.published.isAfter(now) || dto.expired.isBefore(now))
            dto.copy(status = NewsStatus.INACTIVE) else dto.copy(status = NewsStatus.ACTIVE)

    }

    suspend fun findById(id: UUID): NewsRegistrationDTO? {
        return newsRegistrationRepository.findById(id)?.toDTO()
    }

    suspend fun save(news: NewsRegistrationDTO): NewsRegistrationDTO {
        return newsRegistrationRepository.save(news.toEntity()).toDTO()
    }

    suspend fun update(newsRegistration: NewsRegistrationDTO): NewsRegistrationDTO {
        return newsRegistrationRepository.update(newsRegistration.toEntity()).toDTO()
    }

    suspend fun findToBeExpired(
        status: NewsStatus = NewsStatus.ACTIVE,
        expired: LocalDateTime = LocalDateTime.now()
    ): List<NewsRegistrationDTO> {
        return newsRegistrationRepository.findByStatusAndExpiredBefore(status, expired).map { it.toDTO() }
    }

    suspend fun findByToBePublished(
        status: NewsStatus = NewsStatus.INACTIVE,
        expired: LocalDateTime = LocalDateTime.now(),
        published: LocalDateTime = LocalDateTime.now()
    ): List<NewsRegistrationDTO> {
        return newsRegistrationRepository.findByStatusAndExpiredAfterAndPublishedBefore(status, expired, published)
            .map { it.toDTO() }
    }

    open suspend fun findAll(spec: PredicateSpecification<NewsRegistration>?,
                        pageable: Pageable): Page<NewsRegistrationDTO> =
        newsRegistrationRepository.findAll(spec, pageable).map { it.toDTO() }

}