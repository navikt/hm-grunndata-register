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
import no.nav.hm.grunndata.register.product.batch.ProductRegistrationExcelDTO
import no.nav.hm.grunndata.register.product.batch.toProductRegistrationDryRunDTO
import no.nav.hm.grunndata.register.product.batch.toRegistrationDTO
import no.nav.hm.grunndata.register.product.batch.toRegistrationDryRunDTO
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistration
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationRepository
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

    open suspend fun findById(id: UUID) = productRegistrationRepository.findById(id)?.toDTO()

    open suspend fun findByIdIn(ids: List<UUID>) = productRegistrationRepository.findByIdIn(ids).map { it.toDTO() }

    open suspend fun findByHmsArtNr(hmsArtNr: String) = productRegistrationRepository.findByHmsArtNr(hmsArtNr)?.toDTO()

    open suspend fun findBySupplierRef(supplierRef: String) = productRegistrationRepository.findBySupplierRef(supplierRef)?.toDTO()

    open suspend fun save(dto: ProductRegistrationDTO): ProductRegistrationDTO = productRegistrationRepository.save(dto.toEntity()).toDTO()

    open suspend fun update(dto: ProductRegistrationDTO) = productRegistrationRepository.update(dto.toEntity()).toDTO()

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
    ) = productRegistrationRepository.findBySupplierRefAndSupplierId(supplierRef, supplierId)?.toDTO()

    open suspend fun findBySupplierId(supplierId: UUID) = productRegistrationRepository.findBySupplierId(supplierId).map { it.toDTO() }

    open suspend fun findByIdAndSupplierId(
        id: UUID,
        supplierId: UUID,
    ) = productRegistrationRepository.findByIdAndSupplierId(id, supplierId)?.toDTO()

    @Transactional
    open suspend fun updateProduct(
        registrationDTO: ProductRegistrationDTO,
        id: UUID,
        authentication: Authentication,
    ): ProductRegistrationDTO {
        findByIdAndSupplierId(id, registrationDTO.supplierId)?.let { inDb ->
            val dto =
                saveAndCreateEventIfNotDraftAndApproved(
                    registrationDTO
                        .copy(
                            draftStatus =
                                if (inDb.draftStatus == DraftStatus.DONE && inDb.adminStatus == AdminStatus.APPROVED) {
                                    DraftStatus.DONE
                                } else {
                                    registrationDTO.draftStatus
                                },
                            id = inDb.id,
                            created = inDb.created,
                            updatedBy = REGISTER,
                            updatedByUser = authentication.name,
                            createdByUser = inDb.createdByUser,
                            createdBy = inDb.createdBy,
                            createdByAdmin = inDb.createdByAdmin,
                            adminStatus = inDb.adminStatus,
                            adminInfo = inDb.adminInfo,
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
    open suspend fun updateProductByAdmin(
        registrationDTO: ProductRegistrationDTO,
        id: UUID,
        authentication: Authentication,
    ): ProductRegistrationDTO {
        findById(id)?.let { inDb ->
            val updated =
                saveAndCreateEventIfNotDraftAndApproved(
                    registrationDTO.copy(
                        draftStatus =
                            if (inDb.draftStatus == DraftStatus.DONE && inDb.adminStatus == AdminStatus.APPROVED) {
                                DraftStatus.DONE
                            } else {
                                registrationDTO.draftStatus
                            },
                        adminStatus = inDb.adminStatus,
                        adminInfo = inDb.adminInfo,
                        id = inDb.id,
                        created = inDb.created,
                        updatedByUser = authentication.name,
                        updatedBy = REGISTER,
                        createdBy = inDb.createdBy,
                        createdByAdmin = inDb.createdByAdmin,
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
        dto: ProductRegistrationDTO,
        isUpdate: Boolean,
    ): ProductRegistrationDTO {
        val saved = if (isUpdate) update(dto) else save(dto)
        if (saved.draftStatus == DraftStatus.DONE && saved.adminStatus == AdminStatus.APPROVED) {
            productRegistrationEventHandler.queueDTORapidEvent(saved, eventName = EventName.registeredProductV1)
        }
        return saved
    }

    @Transactional
    open suspend fun saveAllAndCreateEventIfNotDraftAndApproved(
        dtos: List<ProductRegistrationDTO>,
        isUpdate: Boolean,
    ): List<ProductRegistrationDTO> {
        val updated =
            dtos.map {
                val saved = if (isUpdate) update(it) else save(it)
                if (saved.draftStatus == DraftStatus.DONE && saved.adminStatus == AdminStatus.APPROVED) {
                    productRegistrationEventHandler.queueDTORapidEvent(saved, eventName = EventName.registeredProductV1)
                }
                saved
            }

        return updated
    }

    suspend fun findAllBySeriesUuid(seriesUUID: UUID) = productRegistrationRepository.findAllBySeriesUUID(seriesUUID).map { it.toDTO() }

    suspend fun findBySeriesUUIDAndSupplierId(
        seriesId: UUID,
        supplierId: UUID,
    ) = productRegistrationRepository.findBySeriesUUIDAndSupplierId(seriesId, supplierId).map { it.toDTO() }

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
    ): List<ProductRegistrationDTO> {
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
                it.toRegistrationDTO(),
                isUpdate = false,
            )
        }
    }

    private fun changed(
        inDb: ProductRegistrationDTO,
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
                            title = it.produktseriesnavn ?: it.produktnavn,
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
    ): ProductRegistrationDTO {
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
            ProductRegistrationDTO(
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
                agreements = emptyList(),
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
    ): ProductRegistrationDTO {
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
            ProductRegistrationDTO(
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
                agreements = emptyList(),
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
        supplierId: UUID,
        authentication: Authentication,
    ): ProductRegistrationDTO {
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
            ProductRegistrationDTO(
                id = productId,
                seriesUUID = seriesUUID,
                seriesId = seriesUUID.toString(),
                isoCategory = series.isoCategory,
                supplierId = supplierId,
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
                agreements = emptyList(),
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
            seriesId = seriesUUID,
            status = status,
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
            hmsArtNr = if (agreeements.isNotEmpty()) agreeements.first().hmsArtNr else hmsArtNr,
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

    suspend fun findExpired(): List<ProductRegistrationDTO> =
        productRegistrationRepository.findByRegistrationStatusAndExpiredBefore(
            RegistrationStatus.ACTIVE,
            expired = LocalDateTime.now(),
        ).map { it.toDTO() }

    suspend fun findProductsToPublish(): List<ProductRegistrationDTO> =
        productRegistrationRepository.findByRegistrationStatusAndAdminStatusAndPublishedBeforeAndExpiredAfter(
            RegistrationStatus.INACTIVE,
            AdminStatus.APPROVED,
            published = LocalDateTime.now(),
            expired = LocalDateTime.now(),
        ).map { it.toDTO() }

    suspend fun exitsBySeriesUUIDAndSupplierId(
        seriesUUID: UUID,
        supplierId: UUID,
    ) = productRegistrationRepository.existsBySeriesUUIDAndSupplierId(
        seriesUUID,
        supplierId,
    )

    suspend fun deleteAll(dtos: List<ProductRegistrationDTO>) = productRegistrationRepository.deleteAll(dtos.map { it.toEntity() })

    suspend fun findAllBySeriesUUIDAndAdminStatusAndDraftStatusAndRegistrationStatus(
        seriesUUID: UUID,
        adminStatus: AdminStatus,
        draftStatus: DraftStatus,
        registrationStatus: RegistrationStatus,
    ): List<ProductRegistrationDTO> =
        productRegistrationRepository.findAllBySeriesUUIDAndAdminStatusAndDraftStatusAndRegistrationStatus(
            seriesUUID,
            adminStatus,
            draftStatus,
            registrationStatus,
        ).map { it.toDTO() }

    suspend fun findAllBySeriesUUIDAndDraftStatusAndRegistrationStatus(
        seriesUUID: UUID,
        draftStatus: DraftStatus,
        registrationStatus: RegistrationStatus,
    ): List<ProductRegistrationDTO> =
        productRegistrationRepository.findAllBySeriesUUIDAndDraftStatusAndRegistrationStatus(
            seriesUUID,
            draftStatus,
            registrationStatus,
        ).map { it.toDTO() }
}

suspend fun <T : Any, R : Any> Page<T>.mapSuspend(transform: suspend (T) -> R): Page<R> {
    val content = this.content.map { transform(it) }
    return Page.of(content, this.pageable, this.totalSize)
}
