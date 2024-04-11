package no.nav.hm.grunndata.register.series

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import no.nav.hm.grunndata.register.product.mapSuspend
import java.util.UUID

@Singleton
open class SeriesRegistrationService(
    private val seriesRegistrationRepository: SeriesRegistrationRepository,
    private val seriesRegistrationEventHandler: SeriesRegistrationEventHandler,
    private val productRegistrationRepository: ProductRegistrationRepository,
) {
    suspend fun findById(id: UUID): SeriesRegistrationDTO? = seriesRegistrationRepository.findById(id)?.toDTO()

    suspend fun update(
        dto: SeriesRegistrationDTO,
        id: UUID = dto.id,
    ) = seriesRegistrationRepository.update(dto.toEntity()).toDTO()

    suspend fun save(
        dto: SeriesRegistrationDTO,
        id: UUID = dto.id,
    ) = seriesRegistrationRepository.save(dto.toEntity()).toDTO()

    @Transactional
    open suspend fun saveAndCreateEventIfNotDraftAndApproved(
        dto: SeriesRegistrationDTO,
        isUpdate: Boolean,
    ): SeriesRegistrationDTO {
        val saved = if (isUpdate) update(dto) else save(dto)
        if (saved.draftStatus == DraftStatus.DONE && saved.adminStatus == AdminStatus.APPROVED) {
            seriesRegistrationEventHandler.queueDTORapidEvent(saved, eventName = EventName.registeredSeriesV1)
        }
        return saved
    }

    suspend fun findAll(
        spec: PredicateSpecification<SeriesRegistration>?,
        pageable: Pageable,
    ): Page<SeriesRegistrationDTO> = seriesRegistrationRepository.findAll(spec, pageable).mapSuspend { it.toDTO() }

    suspend fun findAllBySupplier(
        spec: PredicateSpecification<SeriesRegistration>?,
        pageable: Pageable,
    ): Page<SeriesRegistrationWithArticleCountDTO> =
        seriesRegistrationRepository.findAll(spec, pageable)
            .mapSuspend { it.toDSeriesRegistrationWithArticleCountDto() }

    suspend fun findByIdAndSupplierId(
        id: UUID,
        supplierId: UUID,
    ): SeriesRegistrationDTO? = seriesRegistrationRepository.findByIdAndSupplierId(id, supplierId)?.toDTO()

    private suspend fun SeriesRegistration.toDSeriesRegistrationWithArticleCountDto(): SeriesRegistrationWithArticleCountDTO {
        val articleCount = productRegistrationRepository.countBySeriesId(id.toString())

        return SeriesRegistrationWithArticleCountDTO(
            id = id,
            supplierId = supplierId,
            identifier = identifier,
            title = title,
            text = text,
            isoCategory = isoCategory,
            draftStatus = draftStatus,
            status = status,
            created = created,
            updated = updated,
            createdBy = createdBy,
            updatedBy = updatedBy,
            updatedByUser = updatedByUser,
            createdByUser = createdByUser,
            createdByAdmin = createdByAdmin,
            seriesData = seriesData,
            version = version,
            articleCount = articleCount,
        )
    }
}
