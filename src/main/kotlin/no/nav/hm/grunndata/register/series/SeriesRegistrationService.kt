package no.nav.hm.grunndata.register.series

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.data.runtime.criteria.where
import io.micronaut.http.HttpResponse
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.security.authentication.Authentication
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactive.asFlow
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.MediaType
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.iso.IsoCategoryService
import no.nav.hm.grunndata.register.media.MediaUploadService
import no.nav.hm.grunndata.register.media.ObjectType
import no.nav.hm.grunndata.register.product.MediaInfoDTO
import no.nav.hm.grunndata.register.product.ProductDTOMapper
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.product.mapSuspend
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationService
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID
import no.nav.hm.grunndata.register.error.ErrorType
import no.nav.hm.grunndata.register.product.isAdmin
import no.nav.hm.grunndata.register.security.supplierId
import no.nav.hm.grunndata.register.series.SeriesRegistrationController.Companion

@Singleton
open class SeriesRegistrationService(
    private val seriesRegistrationRepository: SeriesRegistrationRepository,
    private val productRegistrationService: ProductRegistrationService,
    private val seriesRegistrationEventHandler: SeriesRegistrationEventHandler,
    private val supplierService: SupplierRegistrationService,
    private val isoCategoryService: IsoCategoryService,
    private val productAgreementRegistrationService: ProductAgreementRegistrationService,
    private val mediaUploadService: MediaUploadService,
    private val productDTOMapper: ProductDTOMapper,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesRegistrationService::class.java)
    }

    @Transactional
    open suspend fun moveVariantsToSeries(
        seriesId: UUID,
        productIds: List<UUID>,
        authentication: Authentication,
    ) {
        val seriesTo =
            findById(seriesId)
                ?: throw BadRequestException("Series with id $seriesId does not exist")
        productIds.forEach { productId ->
            productRegistrationService.findById(productId)?.let {
                productRegistrationService.update(
                    it.copy(
                        seriesUUID = seriesId,
                        adminStatus = AdminStatus.PENDING,
                        updatedByUser = authentication.name,
                        updated = LocalDateTime.now(),
                    ),
                )
            }
        }
        update(
            seriesTo.copy(
                adminStatus = AdminStatus.PENDING,
                updated = LocalDateTime.now(),
                updatedByUser = authentication.name,
            ),
        )
    }

    suspend fun findById(id: UUID): SeriesRegistrationDTO? = seriesRegistrationRepository.findById(id)?.toDTO()

    suspend fun findByIdV2(id: UUID): SeriesRegistration? {
        return seriesRegistrationRepository.findByIdAndStatusIn(
            id,
            listOf(SeriesStatus.ACTIVE, SeriesStatus.INACTIVE),
        )
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
    ): SeriesRegistration? {
        return seriesRegistrationRepository.findByIdAndSupplierIdAndStatusIn(
            id,
            supplierId,
            listOf(SeriesStatus.ACTIVE, SeriesStatus.INACTIVE),
        )
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
            root[SeriesRegistration::status] ne SeriesStatus.DELETED
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
            isExpired = expired < LocalDateTime.now(),
        )
    }

    @Transactional
    open suspend fun requestApprovalForSeriesAndVariants(seriesToUpdate: SeriesRegistrationDTO): SeriesRegistrationDTO {
        val updatedSeries =
            seriesToUpdate.copy(
                draftStatus = DraftStatus.DONE,
                adminStatus = AdminStatus.PENDING,
                updatedBy = REGISTER,
            )

        val variantsToUpdate =
            productRegistrationService.findBySeriesUUIDAndSupplierId(
                seriesToUpdate.id,
                seriesToUpdate.supplierId,
            ).map {
                it.copy(
                    draftStatus = DraftStatus.DONE,
                    adminStatus = AdminStatus.PENDING,
                    updatedBy = REGISTER,
                )
            }

        saveAndCreateEventIfNotDraftAndApproved(updatedSeries, true)
        productRegistrationService.saveAllAndCreateEventIfNotDraftAndApproved(variantsToUpdate, true)

        return updatedSeries
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
                updatedBy = REGISTER,
            )

        val variantsToUpdate =
            productRegistrationService.findAllBySeriesUuid(
                seriesToUpdate.id,
            ).map {
                it.copy(
                    draftStatus = DraftStatus.DRAFT,
                    adminStatus = AdminStatus.PENDING,
                    updatedBy = REGISTER,
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
                    draftStatus = DraftStatus.DONE,
                    updatedBy = REGISTER,
                    published = seriesToUpdate.published ?: LocalDateTime.now(),
                ),
                isUpdate = true,
            )

        val variantsToUpdate =
            productRegistrationService.findAllBySeriesUUIDAndAdminStatus(
                seriesToUpdate.id,
                AdminStatus.PENDING,
            ).map {
                it.copy(
                    adminStatus = AdminStatus.APPROVED,
                    draftStatus = DraftStatus.DONE,
                    updatedBy = REGISTER,
                    published = it.published ?: LocalDateTime.now(),
                )
            }

        productRegistrationService.saveAllAndCreateEventIfNotDraftAndApproved(
            variantsToUpdate,
            true,
        )

        return updatedSeries
    }

    @Transactional
    open suspend fun rejectSeriesAndVariants(
        seriesToUpdate: SeriesRegistrationDTO,
        message: String?,
        authentication: Authentication,
    ): SeriesRegistrationDTO {
        val updatedSeries =
            saveAndCreateEventIfNotDraftAndApproved(
                seriesToUpdate.copy(
                    message = message,
                    adminStatus = AdminStatus.REJECTED,
                    updatedBy = REGISTER,
                ),
                isUpdate = true,
            )

        val variantsToUpdate =
            productRegistrationService.findAllBySeriesUUIDAndAdminStatus(
                seriesToUpdate.id,
                AdminStatus.PENDING,
            ).map {
                if (it.adminStatus != AdminStatus.PENDING) throw BadRequestException("product is not pending approval")
                if (it.draftStatus != DraftStatus.DONE) throw BadRequestException("product is not done")
                if (it.registrationStatus == RegistrationStatus.DELETED) {
                    throw BadRequestException(
                        "RegistrationStatus should not be be Deleted",
                    )
                }
                it.copy(
                    adminStatus = AdminStatus.REJECTED,
                    updatedBy = REGISTER,
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
        if (seriesToUpdate.published == null || seriesToUpdate.draftStatus != DraftStatus.DRAFT) {
            throw IllegalArgumentException(
                "series cant be set to expired. published = ${seriesToUpdate.published}, draftstatus: ${seriesToUpdate.draftStatus}}",
            )
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
            productRegistrationService.findAllBySeriesUUIDAndRegistrationStatusAndPublishedIsNotNull(
                seriesToUpdate.id,
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

    open suspend fun patchSeries(
        id: UUID,
        patch: UpdateSeriesRegistrationDTO,
        authentication: Authentication
    ): SeriesRegistrationDTO {
        val inDbSeries = if (authentication.isAdmin()) findById(id) ?: throw BadRequestException(
            "Series not found",
            ErrorType.NOT_FOUND
        ) else findByIdAndSupplierId(id, authentication.supplierId()) ?: throw BadRequestException(
            "Series not found",
            ErrorType.NOT_FOUND
        )
        if (!authentication.isAdmin() && inDbSeries.supplierId != authentication.supplierId()) {
            LOG.warn("SupplierId in request does not match authenticated supplierId")
            throw BadRequestException(
                "SupplierId in request does not match authenticated supplierId",
                ErrorType.UNAUTHORIZED
            )
        }

        val inDbSeriesData = inDbSeries.seriesData
        val inDbSeriesAttributes = inDbSeries.seriesData.attributes

        val seriesDataAttributes = inDbSeries.seriesData.attributes.copy(
            keywords = patch.keywords ?: inDbSeriesAttributes.keywords,
            url = patch.url ?: inDbSeriesAttributes.url,
            compatibleWith = inDbSeriesAttributes.compatibleWith
        )

        val seriesData = inDbSeries.seriesData.copy(
            media = inDbSeriesData.media,
            attributes = seriesDataAttributes
        )

        val saved = saveAndCreateEventIfNotDraftAndApproved(
            inDbSeries
                .copy(
                    title = patch.title ?: inDbSeries.title,
                    text = patch.text ?: inDbSeries.text,
                    seriesData = seriesData,
                    updated = LocalDateTime.now(),
                    updatedByUser = authentication.name
                ),
            true,
        )
        return saved
    }

    suspend fun countBySupplier(supplierId: UUID): Long =
        seriesRegistrationRepository.count(
            where {
                root[SeriesRegistration::supplierId] eq supplierId
                root[SeriesRegistration::status] eq SeriesStatus.ACTIVE
            },
        )

    open suspend fun uploadMediaAndUpdateSeries(
        seriesToUpdate: SeriesRegistrationDTO,
        files: Publisher<CompletedFileUpload>,
    ) {
        val mediaDtos =
            files.asFlow().map { mediaUploadService.uploadMedia(it, seriesToUpdate.id, ObjectType.SERIES) }.toSet()

        var lowestPriority = seriesToUpdate.seriesData.media.let { oldMedia ->
            if (oldMedia.isEmpty()) 0 else oldMedia.maxOf { it.priority }
        }
        val mediaInfos =
            mediaDtos.map {
                MediaInfoDTO(
                    sourceUri = it.sourceUri,
                    filename = it.filename,
                    uri = it.uri,
                    type = it.type,
                    text = it.filename?.substringBeforeLast("."),
                    source = it.source,
                    updated = it.updated,
                    priority = ++lowestPriority
                )
            }.toSet()

        val newMedia = seriesToUpdate.seriesData.media.plus(mediaInfos)
        val newData = seriesToUpdate.seriesData.copy(media = newMedia)
        val newUpdate = seriesToUpdate.copy(seriesData = newData)

        seriesRegistrationRepository.update(newUpdate.toEntity())
    }
}
