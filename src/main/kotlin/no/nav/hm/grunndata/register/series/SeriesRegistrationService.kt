package no.nav.hm.grunndata.register.series

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.event.EventName
import java.util.*

@Singleton
open class SeriesRegistrationService(private val seriesRegistrationRepository: SeriesRegistrationRepository,
                                private val seriesRegistrationEventHandler: SeriesRegistrationEventHandler) {

    suspend fun findById(id: UUID): SeriesRegistrationDTO? = seriesRegistrationRepository.findById(id)?.toDTO()


    suspend fun update(dto: SeriesRegistrationDTO, id: UUID = dto.id) =
        seriesRegistrationRepository.update(dto.toEntity()).toDTO()

    suspend fun save(dto: SeriesRegistrationDTO, id: UUID = dto.id) =
        seriesRegistrationRepository.save(dto.toEntity()).toDTO()

    @Transactional
    open suspend fun saveAndPushToRapidIfNotDraft(dto: SeriesRegistrationDTO, isUpdate: Boolean): SeriesRegistrationDTO {
        val saved = if (isUpdate) update(dto) else save(dto)
        if (saved.draftStatus == DraftStatus.DONE)
            seriesRegistrationEventHandler.queueDTORapidEvent(saved, eventName = EventName.registeredSeriesV1)
        return saved
    }

    suspend fun findAll(spec: PredicateSpecification<SeriesRegistration>?, pageable: Pageable): Page<SeriesRegistrationDTO> =
        seriesRegistrationRepository.findAll(spec, pageable).map { it.toDTO() }


}
