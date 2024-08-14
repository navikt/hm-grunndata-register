package no.nav.hm.grunndata.register.series

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.data.runtime.criteria.where
import io.micronaut.security.authentication.Authentication
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.MediaType
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.iso.IsoCategoryService
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.product.mapSuspend
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationService
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

@Singleton
open class SeriesRegistrationService(
    private val seriesRegistrationRepository: SeriesRegistrationRepository,
    private val productRegistrationService: ProductRegistrationService,
    private val seriesRegistrationEventHandler: SeriesRegistrationEventHandler,
    private val supplierService: SupplierRegistrationService,
    private val isoCategoryService: IsoCategoryService,
    private val productAgreementRegistrationService: ProductAgreementRegistrationService

) {
    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesRegistrationService::class.java)
    }

    suspend fun findById(id: UUID): SeriesRegistrationDTO? = seriesRegistrationRepository.findById(id)?.toDTO()

    suspend fun findByIdV2(
        id: UUID,
    ): SeriesRegistrationDTOV2? {
        return seriesRegistrationRepository.findById(id)?.let{ seriesRegistration ->
            val supplierRegistration = supplierService.findById(seriesRegistration.supplierId)
                ?: throw IllegalArgumentException("cannot find series supplier")

            val isoCategoryDTO = isoCategoryService.lookUpCode(seriesRegistration.isoCategory)
                ?: throw IllegalArgumentException("cannot find series iso code")

            val productRegistrationDTOs =
                productRegistrationService.findAllBySeriesUuidV2(id).sortedBy { it.created }

            val inAgreement =
                productAgreementRegistrationService.findAllByProductIds(
                    productRegistrationService.findAllBySeriesUuid(seriesRegistration.id)
                        .filter { it.registrationStatus == RegistrationStatus.ACTIVE }
                        .map { it.id }
                ).isNotEmpty()

            toSeriesRegistrationDTOV2(
                seriesRegistration = seriesRegistration,
                supplierName = supplierRegistration.name,
                productRegistrationDTOs = productRegistrationDTOs,
                isoCategoryDTO = isoCategoryDTO,
                inAgreement = inAgreement
            )
        }
    }
    open suspend fun findByIdIn(ids: List<UUID>) = seriesRegistrationRepository.findByIdIn(ids).map { it.toDTO() }

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
        eventName: String = EventName.registeredSeriesV1,
    ): SeriesRegistrationDTO {
        val saved = if (isUpdate) update(dto) else save(dto)
        if (saved.draftStatus == DraftStatus.DONE && saved.adminStatus == AdminStatus.APPROVED) {
            seriesRegistrationEventHandler.queueDTORapidEvent(saved, eventName = eventName)
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

    suspend fun findByIdAndSupplierIdV2(
        id: UUID,
        supplierId: UUID,
    ): SeriesRegistrationDTOV2? {
        return seriesRegistrationRepository.findByIdAndSupplierId(id, supplierId)?.let{ seriesRegistration ->
            val supplierRegistration = supplierService.findById(seriesRegistration.supplierId)
                ?: throw IllegalArgumentException("cannot find series supplier")

            val isoCategoryDTO = isoCategoryService.lookUpCode(seriesRegistration.isoCategory)
                ?: throw IllegalArgumentException("cannot find series iso code")

            val productRegistrationDTOs =
                productRegistrationService.findBySeriesUUIDAndSupplierIdV2(id, supplierId).sortedBy { it.created }

            val inAgreement =
                productAgreementRegistrationService.findAllByProductIds(
                    productRegistrationService.findAllBySeriesUuid(seriesRegistration.id)
                        .filter { it.registrationStatus == RegistrationStatus.ACTIVE }
                        .map { it.id }
                ).isNotEmpty()

            toSeriesRegistrationDTOV2(
                seriesRegistration = seriesRegistration,
                supplierName = supplierRegistration.name,
                productRegistrationDTOs = productRegistrationDTOs,
                isoCategoryDTO = isoCategoryDTO,
                inAgreement = inAgreement
            )
        }
    }

    suspend fun findBySupplierId(supplierId: UUID): List<SeriesRegistrationDTO> =
        seriesRegistrationRepository.findBySupplierId(supplierId).map { it.toDTO() }

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

    open suspend fun findSeriesToApprove(
        pageable: Pageable,
        params: java.util.HashMap<String, String>?,
    ): Page<SeriesToApproveDTO> {
        return seriesRegistrationRepository.findAll(buildCriteriaSpecPendingProducts(params), pageable)
            .mapSuspend { it.toSeriesToApproveDTO() }
    }

    private fun buildCriteriaSpecPendingProducts(params: java.util.HashMap<String, String>?): PredicateSpecification<SeriesRegistration> =
        where {
            root[SeriesRegistration::adminStatus] eq AdminStatus.PENDING
            root[SeriesRegistration::status] eq SeriesStatus.ACTIVE
            root[SeriesRegistration::draftStatus] eq DraftStatus.DONE
            params?.let {
                if (it.containsKey("createdByAdmin")) {
                    root[SeriesRegistration::createdByAdmin] eq (it["createdByAdmin"].toBoolean())
                }
            }
        }

    private suspend fun SeriesRegistration.toSeriesToApproveDTO(): SeriesToApproveDTO {
        val status = published?.let { "CHANGE" } ?: "NEW"
        val supplier = supplierService.findById(supplierId)

        return SeriesToApproveDTO(
            title = title,
            supplierName = supplier?.name ?: "",
            seriesUUID = id,
            status = status,
            thumbnail = seriesData.media.firstOrNull { it.type == MediaType.IMAGE },
        )
    }

    @Transactional
    open suspend fun setSeriesToDraftStatus(
        seriesToUpdate: SeriesRegistrationDTO,
        authentication: Authentication,
    ): SeriesRegistrationDTO {
        val updatedSeries =
            seriesToUpdate.copy(
                draftStatus = DraftStatus.DRAFT,
                adminStatus = AdminStatus.PENDING,
                updated = LocalDateTime.now(),
                updatedBy = REGISTER,
                updatedByUser = authentication.name,
            )

        val variantsToUpdate =
            productRegistrationService.findAllBySeriesUUIDAndDraftStatusAndRegistrationStatus(
                seriesToUpdate.id,
                DraftStatus.DONE,
                RegistrationStatus.ACTIVE,
            ).map {
                it.copy(
                    draftStatus = DraftStatus.DRAFT,
                    adminStatus = AdminStatus.PENDING,
                    updated = LocalDateTime.now(),
                    updatedBy = REGISTER,
                    updatedByUser = authentication.name,
                )
            }

        saveAndCreateEventIfNotDraftAndApproved(updatedSeries, true)
        productRegistrationService.saveAllAndCreateEventIfNotDraftAndApproved(variantsToUpdate, true)

        return updatedSeries
    }

    @Transactional
    open suspend fun deleteSeries(
        seriesToDelete: SeriesRegistrationDTO,
        authentication: Authentication,
    ): SeriesRegistrationDTO {
        val updatedSeries =
            seriesToDelete.copy(
                status = SeriesStatus.DELETED,
                expired = LocalDateTime.now(),
                updatedByUser = authentication.name,
                updatedBy = REGISTER,
                updated = LocalDateTime.now(),
            )

        val deleted = saveAndCreateEventIfNotDraftAndApproved(updatedSeries, isUpdate = true)

        val variantsToDelete =
            productRegistrationService.findAllBySeriesUuid(seriesToDelete.id).map {
                it.copy(
                    registrationStatus = RegistrationStatus.DELETED,
                    expired = LocalDateTime.now(),
                    updatedByUser = authentication.name,
                    updatedBy = REGISTER,
                    updated = LocalDateTime.now(),
                )
            }

        productRegistrationService.saveAllAndCreateEventIfNotDraftAndApproved(variantsToDelete, isUpdate = true)

        return deleted
    }

    @Transactional
    open suspend fun approveManySeriesAndVariants(
        seriesListToUpdate: List<SeriesRegistrationDTO>,
        authentication: Authentication,
    ): List<SeriesRegistrationDTO> =
        seriesListToUpdate.map { series ->
            approveSeriesAndVariants(series, authentication)
        }

    @Transactional
    open suspend fun approveSeriesAndVariants(
        seriesToUpdate: SeriesRegistrationDTO,
        authentication: Authentication,
    ): SeriesRegistrationDTO {
        val updatedSeries =
            saveAndCreateEventIfNotDraftAndApproved(
                seriesToUpdate.copy(
                    message = null,
                    adminStatus = AdminStatus.APPROVED,
                    updated = LocalDateTime.now(),
                    published = seriesToUpdate.published ?: LocalDateTime.now(),
                    updatedBy = REGISTER,
                ),
                isUpdate = true,
            )

        val variantsToUpdate =
            productRegistrationService.findAllBySeriesUUIDAndAdminStatusAndDraftStatusAndRegistrationStatus(
                seriesToUpdate.id,
                AdminStatus.PENDING,
                DraftStatus.DONE,
                RegistrationStatus.ACTIVE,
            ).map {
                it.copy(
                    adminStatus = AdminStatus.APPROVED,
                    published = it.published ?: LocalDateTime.now(),
                    updated = LocalDateTime.now(),
                    updatedBy = REGISTER,
                    updatedByUser = authentication.name,
                )
            }

        productRegistrationService.saveAllAndCreateEventIfNotDraftAndApproved(
            variantsToUpdate,
            true,
        )

        return updatedSeries
    }

    @Transactional
    open suspend fun setPublishedSeriesRegistrationStatus(
        seriesToUpdate: SeriesRegistrationDTO,
        authentication: Authentication,
        status: SeriesStatus,
    ): SeriesRegistrationDTO {
        if (!seriesToUpdate.isPublishedProduct()) {
            throw IllegalArgumentException("series is not published")
        }

        val newExpirationDate =
            if (status == SeriesStatus.INACTIVE) LocalDateTime.now() else LocalDateTime.now().plusYears(10)

        val updatedSeries =
            seriesToUpdate.copy(
                status = status,
                expired = newExpirationDate,
                updated = LocalDateTime.now(),
                updatedBy = REGISTER,
                updatedByUser = authentication.name,
            )

        val oldRegistrationStatus =
            if (status == SeriesStatus.ACTIVE) RegistrationStatus.INACTIVE else RegistrationStatus.ACTIVE
        val newRegistrationStatus =
            if (status == SeriesStatus.ACTIVE) RegistrationStatus.ACTIVE else RegistrationStatus.INACTIVE

        val variantsToUpdate =
            productRegistrationService.findAllBySeriesUUIDAndAdminStatusAndDraftStatusAndRegistrationStatus(
                seriesToUpdate.id,
                AdminStatus.APPROVED,
                DraftStatus.DONE,
                oldRegistrationStatus,
            ).map {
                it.copy(
                    registrationStatus = newRegistrationStatus,
                    expired = newExpirationDate,
                    updated = LocalDateTime.now(),
                    updatedBy = REGISTER,
                    updatedByUser = authentication.name,
                )
            }

        saveAndCreateEventIfNotDraftAndApproved(updatedSeries, true)
        productRegistrationService.saveAllAndCreateEventIfNotDraftAndApproved(
            variantsToUpdate,
            true,
        )

        return updatedSeries
    }

    suspend fun countBySupplier(supplierId: UUID): Long =
        seriesRegistrationRepository.count(
            where {
                root[SeriesRegistration::supplierId] eq supplierId
                root[SeriesRegistration::status] eq SeriesStatus.ACTIVE
            },
        )
}
