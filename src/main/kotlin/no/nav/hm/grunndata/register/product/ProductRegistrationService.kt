package no.nav.hm.grunndata.register.product

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.data.runtime.criteria.where
import io.micronaut.security.authentication.Authentication
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.rapids_rivers.toUUID
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.AgreementInfo
import no.nav.hm.grunndata.rapid.dto.Attributes
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.MediaType
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.TechData
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationService
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.error.ErrorType
import no.nav.hm.grunndata.register.product.batch.ProductRegistrationExcelDTO
import no.nav.hm.grunndata.register.product.batch.toProductRegistrationDryRunDTO
import no.nav.hm.grunndata.register.product.batch.toProductRegistration
import no.nav.hm.grunndata.register.product.batch.toRegistrationDryRunDTO
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistration
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationRepository
import no.nav.hm.grunndata.register.security.supplierId
import no.nav.hm.grunndata.register.series.SeriesRegistrationRepository
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.grunndata.register.techlabel.TechLabelService
import org.slf4j.LoggerFactory

@Singleton
open class ProductRegistrationService(
    private val productRegistrationRepository: ProductRegistrationRepository,
    private val seriesRegistrationRepository: SeriesRegistrationRepository,
    private val productRegistrationEventHandler: ProductRegistrationEventHandler,
    private val techLabelService: TechLabelService,
    private val agreementRegistrationService: AgreementRegistrationService,
    private val productAgreementRegistrationRepository: ProductAgreementRegistrationRepository,
    private val supplierService: SupplierRegistrationService,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(ProductRegistration::class.java)
    }

    open suspend fun findById(id: UUID) = productRegistrationRepository.findById(id)

    open suspend fun findByIdIn(ids: List<UUID>) = productRegistrationRepository.findByIdIn(ids)

    open suspend fun findByHmsArtNr(hmsArtNr: String) =
        productRegistrationRepository.findByHmsArtNrAndRegistrationStatusIn(
            hmsArtNr,
            listOf(RegistrationStatus.ACTIVE, RegistrationStatus.INACTIVE)
        )

    open suspend fun findBySupplierRef(supplierRef: String) =
        productRegistrationRepository.findBySupplierRefAndRegistrationStatusIn(
            supplierRef,
            listOf(RegistrationStatus.ACTIVE, RegistrationStatus.INACTIVE)
        )

    open suspend fun save(dto: ProductRegistration) = productRegistrationRepository.save(dto)
    open suspend fun update(dto: ProductRegistration) = productRegistrationRepository.update(dto)

    open suspend fun findAll(
        spec: PredicateSpecification<ProductRegistration>?,
        pageable: Pageable,
    ): Page<ProductRegistrationDTO> = productRegistrationRepository.findAll(spec, pageable).mapSuspend { it.toDTO() }

    open suspend fun findProductsToApprove(pageable: Pageable): Page<ProductToApproveDto> =
        productRegistrationRepository.findAll(buildCriteriaSpecPendingProducts(), pageable)
            .mapSuspend { it.toProductToApproveDto() }

    private fun buildCriteriaSpecPendingProducts(): PredicateSpecification<ProductRegistration> =
        where {
            root[ProductRegistration::adminStatus] eq AdminStatus.PENDING
            root[ProductRegistration::registrationStatus] eq RegistrationStatus.ACTIVE
            root[ProductRegistration::draftStatus] eq DraftStatus.DONE
        }

    open suspend fun findBySupplierRefAndSupplierId(
        supplierRef: String,
        supplierId: UUID,
    ) = productRegistrationRepository.findBySupplierRefAndSupplierId(supplierRef, supplierId)

    open suspend fun findBySupplierId(supplierId: UUID) = productRegistrationRepository.findBySupplierId(supplierId)

    open suspend fun findByIdAndSupplierId(
        id: UUID,
        supplierId: UUID,
    ) = productRegistrationRepository.findByIdAndSupplierId(id, supplierId)

    @Transactional
    open suspend fun updateProductBySupplierV2(
        updateDTO: UpdateProductRegistrationDTO,
        id: UUID,
        authentication: Authentication,
    ): ProductRegistration {
        findByIdAndSupplierId(id, authentication.supplierId())?.let { inDb ->
            if (authentication.supplierId() != inDb.supplierId) throw BadRequestException(
                "product belongs to another supplier",
                type = ErrorType.UNAUTHORIZED
            )

            val dto =
                saveAndCreateEventIfNotDraftAndApproved(
                    inDb.copy(
                        hmsArtNr = updateDTO.hmsArtNr,
                        articleName = updateDTO.articleName,
                        supplierRef = if (inDb.published != null) inDb.supplierRef else updateDTO.supplierRef, // supplierRef cannot be changed by supplier if published,
                        productData = inDb.productData.copy(
                            attributes = updateDTO.productData.attributes,
                            techData = updateDTO.productData.techData.map { it.toEntity() }
                        ),
                        updatedByUser = authentication.name,
                        updatedBy = REGISTER,
                        updated = LocalDateTime.now(),
                    ),
                    isUpdate = true,
                )

            val series = seriesRegistrationRepository.findById(dto.seriesUUID)
            if (series != null) {
                val updatedSeries =
                    series.copy(
                        updated = LocalDateTime.now(),
                        updatedByUser = authentication.name,
                        updatedBy = REGISTER,
                    )
                seriesRegistrationRepository.update(updatedSeries)
            }
            return dto
        } ?: run {
            throw BadRequestException("Product does not exists $id")
        }
    }

    @Transactional
    open suspend fun updateProductByAdminV2(
        updateDTO: UpdateProductRegistrationDTO,
        id: UUID,
        authentication: Authentication,
    ): ProductRegistration {
        findById(id)?.let { inDb ->
            var changedHmsNrSupplierRef = false
            if (inDb.hmsArtNr != updateDTO.hmsArtNr) {
                LOG.warn("Hms artNr ${inDb.hmsArtNr} has been changed by admin to ${updateDTO.hmsArtNr} for id: $id")
                changedHmsNrSupplierRef = true
            }
            if (inDb.supplierRef != updateDTO.supplierRef) {
                LOG.warn("Supplier ref ${inDb.supplierRef} has been changed by admin to ${updateDTO.supplierRef} for id: $id")
                changedHmsNrSupplierRef = true
            }
            if (changedHmsNrSupplierRef) {
                productAgreementRegistrationRepository.findBySupplierIdAndSupplierRef(inDb.supplierId, inDb.supplierRef)
                    .forEach { change ->
                        productAgreementRegistrationRepository.update(
                            change.copy(
                                hmsArtNr = updateDTO.hmsArtNr,
                                supplierRef = updateDTO.supplierRef
                            )
                        )
                    }
            }

            val updated =
                saveAndCreateEventIfNotDraftAndApproved(
                    inDb.copy(
                        hmsArtNr = updateDTO.hmsArtNr,
                        articleName = updateDTO.articleName,
                        supplierRef = updateDTO.supplierRef,
                        productData = inDb.productData.copy(
                            attributes = updateDTO.productData.attributes,
                            techData = updateDTO.productData.techData.map { it.toEntity() }
                        ),
                        updatedByUser = authentication.name,
                        updatedBy = REGISTER,
                        updated = LocalDateTime.now(),
                    ),
                    isUpdate = true,
                )

            val series = seriesRegistrationRepository.findById(updated.seriesUUID)
            if (series != null) {
                val updatedSeries =
                    series.copy(
                        updated = LocalDateTime.now(),
                        updatedByUser = authentication.name,
                        updatedBy = REGISTER,
                    )
                seriesRegistrationRepository.update(updatedSeries)
            }
            return updated
        } ?: run {
            throw BadRequestException("Product does not exists $id")
        }
    }

    @Transactional
    open suspend fun saveAndCreateEventIfNotDraftAndApproved(
        dto: ProductRegistration,
        isUpdate: Boolean,
    ): ProductRegistration {
        val saved = if (isUpdate) update(dto) else save(dto)
        if (saved.draftStatus == DraftStatus.DONE && saved.adminStatus == AdminStatus.APPROVED) {
            productRegistrationEventHandler.queueDTORapidEvent(saved.toDTO(), eventName = EventName.registeredProductV1)
        }
        return saved
    }

    @Transactional
    open suspend fun saveAllAndCreateEventIfNotDraftAndApproved(
        dtos: List<ProductRegistration>,
        isUpdate: Boolean,
    ): List<ProductRegistration> {
        val updated =
            dtos.map {
                val saved = if (isUpdate) update(it) else save(it)
                if (saved.draftStatus == DraftStatus.DONE && saved.adminStatus == AdminStatus.APPROVED) {
                    productRegistrationEventHandler.queueDTORapidEvent(
                        saved.toDTO(),
                        eventName = EventName.registeredProductV1
                    )
                }
                saved
            }

        return updated
    }

    suspend fun findAllBySeriesUuid(seriesUUID: UUID) = productRegistrationRepository.findAllBySeriesUUID(seriesUUID)

    suspend fun findBySeriesUUIDAndSupplierId(
        seriesId: UUID,
        supplierId: UUID,
    ) = productRegistrationRepository.findBySeriesUUIDAndSupplierId(seriesId, supplierId)

    suspend fun findSeriesGroup(pageable: Pageable) = seriesRegistrationRepository.findSeriesGroup(pageable)

    open suspend fun createProductVariant(
        id: UUID,
        dto: DraftVariantDTO,
        authentication: Authentication,
    ) = findById(id)?.let {
        val productId = UUID.randomUUID()
        save(
            it.copy(
                supplierRef = dto.supplierRef,
                articleName = dto.articleName,
                id = productId,
                hmsArtNr = null,
                draftStatus = DraftStatus.DRAFT,
                adminStatus = AdminStatus.PENDING,
                registrationStatus = RegistrationStatus.ACTIVE,
                message = null,
                adminInfo = null,
                created = LocalDateTime.now(),
                updated = LocalDateTime.now(),
                published = LocalDateTime.now(),
                expired = LocalDateTime.now().plusYears(10),
                updatedByUser = authentication.name,
                createdByUser = authentication.name,
                createdByAdmin = authentication.isAdmin(),
            ),
        )
    }

    open suspend fun importExcelRegistrations(
        dtos: List<ProductRegistrationExcelDTO>,
        authentication: Authentication,
    ): List<ProductRegistration> {
        return dtos.map {
            findBySupplierRefAndSupplierId(it.levartnr, it.leverandorid.toUUID())?.let { inDb ->
                if (changed(inDb, it)) {
                    val product =
                        inDb.copy(
                            draftStatus = DraftStatus.DRAFT,
                            adminStatus = AdminStatus.PENDING,
                            title = it.produktseriesnavn,
                            articleName = it.produktnavn,
                            isoCategory = it.isoCategory,
                            productData =
                            inDb.productData.copy(
                                techData = it.techData,
                                attributes =
                                inDb.productData.attributes.copy(
                                    shortdescription = it.andrespesifikasjoner,
                                ),
                            ),
                            updated = LocalDateTime.now(),
                            updatedByUser = authentication.name,
                        )
                    saveAndCreateEventIfNotDraftAndApproved(product, isUpdate = true)
                } else {
                    inDb
                }
            } ?: saveAndCreateEventIfNotDraftAndApproved(
                it.toProductRegistration(),
                isUpdate = false,
            )
        }
    }

    private fun changed(
        inDb: ProductRegistration,
        excel: ProductRegistrationExcelDTO,
    ): Boolean {
        if (excel.produktnavn != inDb.articleName) return true
        if (excel.techData != inDb.productData.techData) return true
        if (excel.andrespesifikasjoner != inDb.productData.attributes.shortdescription) return true
        LOG.info("No changes in product ${inDb.id}")
        return false
    }

    open suspend fun importDryRunExcelRegistrations(
        dtos: List<ProductRegistrationExcelDTO>,
        authentication: Authentication,
    ): List<ProductRegistrationDryRunDTO> {
        return dtos.map {
            findBySupplierRefAndSupplierId(it.levartnr, it.leverandorid.toUUID())?.let { inDb ->
                val product =
                    if (changed(inDb, it)) {
                        inDb.copy(
                            draftStatus = DraftStatus.DRAFT,
                            adminStatus = AdminStatus.PENDING,
                            title = it.produktseriesnavn,
                            articleName = it.produktnavn,
                            isoCategory = it.isoCategory,
                            productData =
                            inDb.productData.copy(
                                techData = it.techData,
                                attributes =
                                inDb.productData.attributes.copy(
                                    shortdescription = it.andrespesifikasjoner,
                                ),
                            ),
                            updated = LocalDateTime.now(),
                            updatedByUser = authentication.name,
                        )
                    } else {
                        inDb
                    }
                product.toProductRegistrationDryRunDTO()
            } ?: it.toRegistrationDryRunDTO()
        }
    }

    open suspend fun createDraft(
        supplierId: UUID,
        authentication: Authentication,
        isAccessory: Boolean,
        isSparePart: Boolean,
    ): ProductRegistration {
        val productId = UUID.randomUUID()
        val product =
            ProductData(
                attributes =
                Attributes(
                    shortdescription = "",
                    text = "en lang beskrivelse",
                ),
            )
        val registration =
            ProductRegistration(
                id = productId,
                seriesUUID = productId, // we just use the productId as seriesUUID
                seriesId = productId.toString(),
                isoCategory = "0",
                supplierId = supplierId,
                supplierRef = productId.toString(),
                hmsArtNr = null,
                title = "",
                articleName = "",
                createdBy = REGISTER,
                updatedBy = REGISTER,
                message = null,
                published = LocalDateTime.now(),
                expired = LocalDateTime.now().plusYears(10),
                productData = product,
                createdByUser = authentication.name,
                updatedByUser = authentication.name,
                createdByAdmin = authentication.isAdmin(),
                version = 0,
                sparePart = false,
                accessory = false,
            )
        val draft = save(registration)
        LOG.info("Draft was created ${draft.id} by $supplierId")
        return draft
    }

    open suspend fun createDraftWith(
        supplierId: UUID,
        authentication: Authentication,
        isAccessory: Boolean,
        isSparePart: Boolean,
        draftWithDTO: ProductDraftWithDTO,
    ): ProductRegistration {
        val productId = UUID.randomUUID()
        val product =
            ProductData(
                techData = createTechDataDraft(draftWithDTO),
                attributes =
                Attributes(
                    shortdescription = "",
                    text = draftWithDTO.text,
                ),
            )
        val registration =
            ProductRegistration(
                id = productId,
                seriesUUID = productId, // we just use the productId as seriesUUID
                seriesId = productId.toString(),
                isoCategory = draftWithDTO.isoCategory,
                supplierId = supplierId,
                supplierRef = productId.toString(),
                hmsArtNr = null,
                title = draftWithDTO.title,
                articleName = "",
                createdBy = REGISTER,
                updatedBy = REGISTER,
                message = null,
                published = LocalDateTime.now(),
                expired = LocalDateTime.now().plusYears(10),
                productData = product,
                createdByUser = authentication.name,
                updatedByUser = authentication.name,
                createdByAdmin = authentication.isAdmin(),
                version = 0,
                accessory = false,
                sparePart = false
            )
        val draft = save(registration)
        LOG.info("Draft was created ${draft.id} by $supplierId")
        return draft
    }

    open suspend fun createDraftWithV2(
        seriesUUID: UUID,
        draftWithDTO: DraftVariantDTO,
        authentication: Authentication,
    ): ProductRegistration {
        val previousProduct = productRegistrationRepository.findLastProductInSeries(seriesUUID)

        val series =
            seriesRegistrationRepository.findById(seriesUUID)
                ?: throw RuntimeException("Series not found") // consider caching series
        val productId = UUID.randomUUID()
        val product =
            ProductData(
                techData =
                previousProduct?.productData?.techData ?: createTechDataDraft(series.isoCategory),
                attributes =
                Attributes(
                    shortdescription = "",
                    text = "",
                ),
            )
        val registration =
            ProductRegistration(
                id = productId,
                seriesUUID = seriesUUID,
                seriesId = seriesUUID.toString(),
                isoCategory = series.isoCategory,
                supplierId = series.supplierId,
                supplierRef = draftWithDTO.supplierRef,
                hmsArtNr = null,
                articleName = draftWithDTO.articleName,
                createdBy = REGISTER,
                updatedBy = REGISTER,
                message = null,
                published = null,
                expired = LocalDateTime.now().plusYears(10),
                productData = product,
                createdByUser = authentication.name,
                updatedByUser = authentication.name,
                createdByAdmin = authentication.isAdmin(),
                version = 0,
                accessory = false,
                sparePart = false
            )
        val draft = save(registration)
        LOG.info("Draft was created ${draft.id}")
        return draft
    }

    private fun createTechDataDraft(draftWithDTO: ProductDraftWithDTO): List<TechData> =
        techLabelService.fetchLabelsByIsoCode(draftWithDTO.isoCategory).map {
            TechData(key = it.label, value = "", unit = it.unit ?: "")
        }

    private fun createTechDataDraft(isoCode: String): List<TechData> =
        techLabelService.fetchLabelsByIsoCode(isoCode).map {
            TechData(key = it.label, value = "", unit = it.unit ?: "")
        }

    private suspend fun ProductRegistration.toProductToApproveDto(): ProductToApproveDto {
        val agreeements = productAgreementRegistrationRepository.findBySupplierIdAndSupplierRef(supplierId, supplierRef)
        val agreementInfo = agreeements.map { it.toAgreementInfo() }
        val supplier = supplierService.findById(supplierId)

        val status = if (isDraft()) "NEW" else "EXISTING"

        return ProductToApproveDto(
            title = articleName,
            articleName = articleName,
            supplierName = supplier?.name ?: "",
            agreementId = agreeements.firstOrNull()?.agreementId,
            delkontrakttittel = agreementInfo.firstOrNull()?.title,
            productId = id,
            seriesId = seriesUUID,
            status = status,
            sparePart = sparePart,
            accessory = accessory,
            thumbnail = productData.media.firstOrNull { it.type == MediaType.IMAGE },
        )
    }

    private suspend fun ProductRegistration.toDTO(): ProductRegistrationDTO {
        // TODO cache agreements
        val agreeements = productAgreementRegistrationRepository.findBySupplierIdAndSupplierRef(supplierId, supplierRef)
        return ProductRegistrationDTO(
            id = id,
            supplierId = supplierId,
            seriesId = seriesId,
            seriesUUID = seriesUUID,
            supplierRef = supplierRef,
            hmsArtNr = if (!hmsArtNr.isNullOrBlank()) hmsArtNr else if (agreeements.isNotEmpty()) agreeements.first().hmsArtNr else null,
            title = title,
            articleName = articleName,
            draftStatus = draftStatus,
            adminStatus = adminStatus,
            registrationStatus = registrationStatus,
            message = message,
            adminInfo = adminInfo,
            created = created,
            updated = updated,
            published = published,
            expired = expired,
            updatedByUser = updatedByUser,
            createdByUser = createdByUser,
            createdBy = createdBy,
            updatedBy = updatedBy,
            createdByAdmin = createdByAdmin,
            productData = productData,
            sparePart = sparePart,
            accessory = accessory,
            isoCategory = isoCategory,
            agreements = agreeements.map { it.toAgreementInfo() },
            version = version,
        )
    }

    private suspend fun ProductAgreementRegistration.toAgreementInfo(): AgreementInfo {
        val agreement =
            agreementRegistrationService.findById(agreementId)
                ?: throw RuntimeException("Agreement not found") // consider caching agreements
        val delKontrakt = if (postId != null) agreement.delkontraktList.find { postId == it.id } else null
        return AgreementInfo(
            id = agreementId,
            title = agreement.title,
            identifier = agreement.agreementData.identifier,
            rank = rank,
            postNr = post,
            postIdentifier = delKontrakt?.identifier,
            postId = postId,
            refNr = delKontrakt?.delkontraktData?.refNr,
            published = published,
            expired = expired,
            reference = reference,
            postTitle = delKontrakt?.delkontraktData?.title ?: "",
            status = status,
        )
    }

    suspend fun findExpired(): List<ProductRegistration> =
        productRegistrationRepository.findByRegistrationStatusAndExpiredBefore(
            RegistrationStatus.ACTIVE,
            expired = LocalDateTime.now(),
        )

    suspend fun findProductsToPublish(): List<ProductRegistration> =
        productRegistrationRepository.findByRegistrationStatusAndAdminStatusAndPublishedBeforeAndExpiredAfter(
            RegistrationStatus.INACTIVE,
            AdminStatus.APPROVED,
            published = LocalDateTime.now(),
            expired = LocalDateTime.now(),
        )

    suspend fun exitsBySeriesUUIDAndSupplierId(
        seriesUUID: UUID,
        supplierId: UUID,
    ) = productRegistrationRepository.existsBySeriesUUIDAndSupplierId(
        seriesUUID,
        supplierId,
    )

    suspend fun deleteAll(products: List<ProductRegistration>) =
        productRegistrationRepository.deleteAll(products)

    suspend fun findAllBySeriesUUIDAndRegistrationStatusAndPublishedIsNotNull(
        seriesUUID: UUID,
        registrationStatus: RegistrationStatus,
    ): List<ProductRegistration> =
        productRegistrationRepository.findAllBySeriesUUIDAndRegistrationStatusAndPublishedIsNotNull(
            seriesUUID,
            registrationStatus,
        )

    suspend fun findAllBySeriesUUIDAndAdminStatus(
        seriesUUID: UUID,
        adminStatus: AdminStatus,
    ): List<ProductRegistration> =
        productRegistrationRepository.findAllBySeriesUUIDAndAdminStatus(
            seriesUUID,
            adminStatus,
        )

    suspend fun countBySupplier(supplierId: UUID): Long = productRegistrationRepository.count(
        where {
            root[ProductRegistration::supplierId] eq supplierId
            root[ProductRegistration::registrationStatus] eq RegistrationStatus.ACTIVE
        })
}

suspend fun <T : Any, R : Any> Page<T>.mapSuspend(transform: suspend (T) -> R): Page<R> {
    val content = this.content.map { transform(it) }
    return Page.of(content, this.pageable, this.totalSize)
}
