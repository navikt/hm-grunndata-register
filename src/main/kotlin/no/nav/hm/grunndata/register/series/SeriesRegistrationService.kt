package no.nav.hm.grunndata.register.series

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.security.authentication.Authentication
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.REGISTER
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

@Singleton
open class SeriesRegistrationService(
    private val seriesRegistrationRepository: SeriesRegistrationRepository,
    private val seriesRegistrationEventHandler: SeriesRegistrationEventHandler,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesRegistrationService::class.java)
    }

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
    ): Page<SeriesRegistrationDTO> = seriesRegistrationRepository.findAll(spec, pageable).map { it.toDTO() }

    suspend fun findByIdAndSupplierId(
        id: UUID,
        supplierId: UUID,
    ): SeriesRegistrationDTO? = seriesRegistrationRepository.findByIdAndSupplierId(id, supplierId)?.toDTO()

    suspend fun createDraftWith(
        supplierId: UUID,
        authentication: Authentication,
        draftWithDTO: SeriesDraftWithDTO,
    ): SeriesRegistrationDTO {
        val id = UUID.randomUUID()
        val series =
            SeriesRegistrationDTO(
                id = id,
                supplierId = supplierId,
                isoCategory = draftWithDTO.isoCategory,
                title = draftWithDTO.title,
                text = "",
                identifier = id.toString(),
                draftStatus = DraftStatus.DRAFT,
                adminStatus = AdminStatus.PENDING,
                status = SeriesStatus.ACTIVE,
                createdBy = REGISTER,
                updatedBy = REGISTER,
                createdByUser = authentication.name,
                updatedByUser = authentication.name,
                created = LocalDateTime.now(),
                updated = LocalDateTime.now(),
                seriesData = SeriesDataDTO(media = emptySet()),
                version = 0,
            )
        val draft = save(series)
        LOG.info("Created draft series with id $id for supplier $supplierId")
        return draft
    }

    suspend fun createDraft(
        supplierId: UUID,
        authentication: Authentication,
    ): SeriesRegistrationDTO? {
        val id = UUID.randomUUID()
        val series =
            SeriesRegistrationDTO(
                id = id,
                supplierId = supplierId,
                isoCategory = "0",
                title = "",
                text = "",
                identifier = id.toString(),
                draftStatus = DraftStatus.DRAFT,
                adminStatus = AdminStatus.PENDING,
                status = SeriesStatus.ACTIVE,
                createdBy = REGISTER,
                updatedBy = REGISTER,
                created = LocalDateTime.now(),
                updated = LocalDateTime.now(),
                seriesData = SeriesDataDTO(media = emptySet()),
                version = 0,
            )
        val draft = save(series)
        LOG.info("Created draft series with id $id for supplier $supplierId")
        return draft
    }
}
