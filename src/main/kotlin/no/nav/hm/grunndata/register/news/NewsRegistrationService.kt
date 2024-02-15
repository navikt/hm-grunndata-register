package no.nav.hm.grunndata.register.news

import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.event.EventName

@Singleton
open class NewsRegistrationService(private val newsRegistrationRepository: NewsRegistrationRepository,
                              private val newsRegistrationEventHandler: NewsRegistrationEventHandler) {

    @Transactional
    open suspend fun saveAndCreateEventIfNotDraft(dto: NewsRegistrationDTO, isUpdate: Boolean): NewsRegistrationDTO {
        val saved = if (isUpdate) update(dto) else save(dto)
        if (saved.draftStatus  == DraftStatus.DONE) {
            newsRegistrationEventHandler.queueDTORapidEvent(saved, eventName = EventName.registeredNewsV1)
        }
        return saved
    }

    suspend fun save(news: NewsRegistrationDTO): NewsRegistrationDTO {
        return newsRegistrationRepository.save(news.toEntity()).toDTO()
    }

    suspend fun update(newsRegistration: NewsRegistrationDTO): NewsRegistrationDTO {
        return newsRegistrationRepository.update(newsRegistration.toEntity()).toDTO()
    }
}