package no.nav.hm.grunndata.register.product

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.data.runtime.criteria.where
import io.micronaut.http.annotation.PathVariable
import io.micronaut.security.authentication.Authentication
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
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
import no.nav.hm.grunndata.register.product.batch.toProductRegistration
import no.nav.hm.grunndata.register.product.batch.toProductRegistrationDryRunDTO
import no.nav.hm.grunndata.register.product.batch.toRegistrationDryRunDTO
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistration
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationRepository
import no.nav.hm.grunndata.register.security.supplierId
import no.nav.hm.grunndata.register.series.SeriesRegistrationRepository
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.grunndata.register.techlabel.TechLabelService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

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

    open suspend fun save(dto: ProductRegistration) = productRegistrationRepository.save(dto)

    open suspend fun update(dto: ProductRegistration) = productRegistrationRepository.update(dto)

    open suspend fun findAccessoryOrSparePartButNoCompatibleWith() =
        productRegistrationRepository.findAccessoryOrSparePartButNoCompatibleWith()

    open suspend fun findByHmsArtNr(hmsArtNr: String) =
        productRegistrationRepository.findByHmsArtNrStartingWithAndRegistrationStatusIn(
            hmsArtNr,
            listOf(RegistrationStatus.ACTIVE, RegistrationStatus.INACTIVE),
        )

    open suspend fun findByExactHmsArtNr(hmsArtNr: String) =
        productRegistrationRepository.findByHmsArtNrAndRegistrationStatusIn(
            hmsArtNr,
            listOf(RegistrationStatus.ACTIVE, RegistrationStatus.INACTIVE),
        )

    open suspend fun findByExactHmsArtNrAndSupplierId(hmsArtNr: String, supplierId: UUID) =
        productRegistrationRepository.findByHmsArtNrAndRegistrationStatusInAndSupplierId(
            hmsArtNr,
            listOf(RegistrationStatus.ACTIVE, RegistrationStatus.INACTIVE),
            supplierId
        )

    open suspend fun findByHmsArtNr(hmsArtNr: String, authentication: Authentication): ProductRegistration? =
        if (authentication.isSupplier()) {
            productRegistrationRepository.findByHmsArtNrStartingWithAndRegistrationStatusInAndSupplierId(
                hmsArtNr,
                listOf(RegistrationStatus.ACTIVE, RegistrationStatus.INACTIVE),
                authentication.supplierId(),
            )
        } else {
            productRegistrationRepository.findByHmsArtNrStartingWithAndRegistrationStatusIn(
                hmsArtNr,
                listOf(RegistrationStatus.ACTIVE, RegistrationStatus.INACTIVE),
            )
        }

    open suspend fun findPartByHmsArtNr(hmsArtNr: String, authentication: Authentication): ProductRegistration? =
        if (authentication.isSupplier()) {
            productRegistrationRepository.findPartByHmsArtNrStartingWithAndRegistrationStatusInAndSupplierId(
                hmsArtNr,
                listOf(RegistrationStatus.ACTIVE, RegistrationStatus.INACTIVE),
                authentication.supplierId(),
            )
        } else {
            productRegistrationRepository.findPartByHmsArtNrStartingWithAndRegistrationStatusIn(
                hmsArtNr,
                listOf(RegistrationStatus.ACTIVE, RegistrationStatus.INACTIVE),
            )
        }

    open suspend fun findBySupplierRef(supplierRef: String, authentication: Authentication) =
        if (authentication.isSupplier()) {
            productRegistrationRepository.findBySupplierRefStartingWithAndRegistrationStatusInAndSupplierId(
                supplierRef, listOf(RegistrationStatus.ACTIVE, RegistrationStatus.INACTIVE), authentication.supplierId()
            )
        } else {
            productRegistrationRepository.findBySupplierRefAndRegistrationStatusIn(
                supplierRef,
                listOf(RegistrationStatus.ACTIVE, RegistrationStatus.INACTIVE),
            )
        }

    open suspend fun findPartBySupplierRef(supplierRef: String, authentication: Authentication) =
        if (authentication.isSupplier()) {
            productRegistrationRepository.findPartBySupplierRefStartingWithAndRegistrationStatusInAndSupplierId(
                supplierRef, listOf(RegistrationStatus.ACTIVE, RegistrationStatus.INACTIVE), authentication.supplierId()
            )
        } else {
            productRegistrationRepository.findPartBySupplierRefAndRegistrationStatusIn(
                supplierRef,
                listOf(RegistrationStatus.ACTIVE, RegistrationStatus.INACTIVE),
            )
        }

    open suspend fun findAll(
        spec: PredicateSpecification<ProductRegistration>?,
        pageable: Pageable,
    ): Page<ProductRegistration> = productRegistrationRepository.findAll(spec, pageable)

    open suspend fun findProductsToApprove(pageable: Pageable): Page<ProductToApproveDto> =
        productRegistrationRepository.findAll(
            where {
                root[ProductRegistration::adminStatus] eq AdminStatus.PENDING
                root[ProductRegistration::registrationStatus] eq RegistrationStatus.ACTIVE
                root[ProductRegistration::draftStatus] eq DraftStatus.DONE
            },
            pageable,
        )
            .mapSuspend { it.toProductToApproveDto() }

    open suspend fun findBySupplierRefAndSupplierId(
        supplierRef: String,
        supplierId: UUID,
    ) = productRegistrationRepository.findBySupplierRefAndSupplierId(supplierRef, supplierId)

    open suspend fun findBySupplierId(supplierId: UUID) = productRegistrationRepository.findBySupplierId(supplierId)

    open suspend fun findByIdAndSupplierId(
        id: UUID,
        supplierId: UUID,
    ) = productRegistrationRepository.findByIdAndSupplierId(id, supplierId)

    suspend fun findAllBySeriesUuid(seriesUUID: UUID) =
        productRegistrationRepository.findAllBySeriesUUIDOrderByCreatedAsc(seriesUUID)

    suspend fun findBySeriesUUIDAndSupplierId(
        seriesId: UUID,
        supplierId: UUID,
    ) = productRegistrationRepository.findBySeriesUUIDAndSupplierId(seriesId, supplierId)

    suspend fun findSeriesGroup(pageable: Pageable) = seriesRegistrationRepository.findSeriesGroup(pageable)

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

    @Transactional
    open suspend fun updateProduct(
        updateDTO: UpdateProductRegistrationDTO,
        id: UUID,
        authentication: Authentication,
    ): ProductRegistration {
        findById(id)?.let { inDb ->
            if (!authentication.isAdmin() && authentication.supplierId() != inDb.supplierId) {
                throw BadRequestException("product belongs to another supplier", type = ErrorType.UNAUTHORIZED)
            }

            var changedHmsNrSupplierRef = false
            if ((inDb.hmsArtNr ?: "") != (updateDTO.hmsArtNr ?: "")) {
                if (!inDb.canChangeHmsArtNr(authentication)) {
                    throw BadRequestException("not authorized to change hms number", ErrorType.UNAUTHORIZED)
                }
                LOG.warn("Hms artNr ${inDb.hmsArtNr} has been changed by admin to ${updateDTO.hmsArtNr} for id: $id")
                changedHmsNrSupplierRef = true
            }
            if (inDb.supplierRef != updateDTO.supplierRef) {
                if (!inDb.canChangeSupplierRef(authentication)) {
                    throw BadRequestException("not authorized to change supplierref", ErrorType.UNAUTHORIZED)
                }
                LOG.warn("Supplier ref ${inDb.supplierRef} has been changed by admin to ${updateDTO.supplierRef} for id: $id")
                changedHmsNrSupplierRef = true
            }
            if (changedHmsNrSupplierRef) {
                productAgreementRegistrationRepository.findBySupplierIdAndSupplierRef(
                    inDb.supplierId,
                    inDb.supplierRef,
                ).forEach { change ->
                    productAgreementRegistrationRepository.update(
                        change.copy(hmsArtNr = updateDTO.hmsArtNr, supplierRef = updateDTO.supplierRef),
                    )
                }
            }

            val dto =
                saveAndCreateEventIfNotDraftAndApproved(
                    inDb.copy(
                        hmsArtNr = updateDTO.hmsArtNr,
                        articleName = updateDTO.articleName,
                        supplierRef = updateDTO.supplierRef,
                        productData =
                            inDb.productData.copy(
                                attributes = updateDTO.productData.attributes,
                                techData = updateDTO.productData.techData.map { it.toEntity() },
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
    ): List<ProductRegistration> = dtos.map { saveAndCreateEventIfNotDraftAndApproved(it, isUpdate) }

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
        seriesUUID: UUID,
        draftWithDTO: DraftVariantDTO,
        authentication: Authentication,
    ): ProductRegistration {
        val previousProduct = productRegistrationRepository.findLastProductInSeries(seriesUUID)

        val series =
            seriesRegistrationRepository.findById(seriesUUID)
                ?: throw BadRequestException("Series $seriesUUID not found", type = ErrorType.NOT_FOUND)

        if (authentication.isSupplier() && series.supplierId != authentication.supplierId()) {
            throw BadRequestException(
                "series $seriesUUID does not belong to supplier ${authentication.supplierId()}",
                type = ErrorType.UNAUTHORIZED,
            )
        }

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
                sparePart = false,
                mainProduct = true,
            )
        val draft = save(registration)
        LOG.info("Draft was created ${draft.id}")
        return draft
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
            hmsArtNr = if (agreeements.isNotEmpty() && agreeements.first().hmsArtNr != null) {
                agreeements.first().hmsArtNr
            } else hmsArtNr,
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
            mainProduct = mainProduct,
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

    suspend fun exitsBySeriesUUIDAndSupplierId(
        seriesUUID: UUID,
        supplierId: UUID,
    ) = productRegistrationRepository.existsBySeriesUUIDAndSupplierId(
        seriesUUID,
        supplierId,
    )

    private suspend fun deleteAll(products: List<ProductRegistration>) =
        productRegistrationRepository.deleteAll(products)

    suspend fun countBySupplier(supplierId: UUID): Long =
        productRegistrationRepository.count(
            where {
                root[ProductRegistration::supplierId] eq supplierId
                root[ProductRegistration::registrationStatus] eq RegistrationStatus.ACTIVE
            },
        )

    suspend fun updateExpiredStatus(
        @PathVariable id: UUID,
        authentication: Authentication,
        isExpired: Boolean
    ): ProductRegistration {
        val productToUpdate = findById(id) ?: throw BadRequestException("Product not found", type = ErrorType.NOT_FOUND)

        if (authentication.isSupplier() && productToUpdate.supplierId != authentication.supplierId()) {
            throw BadRequestException("product belongs to another supplier", type = ErrorType.UNAUTHORIZED)
        }

        val newRegistrationStatus = if (isExpired) RegistrationStatus.INACTIVE else RegistrationStatus.ACTIVE
        val newExpiredDate = if (isExpired) {
            LocalDateTime.now()
        } else {
            LocalDateTime.now().plusYears(10)
        }

        val updatedProduct =
            productToUpdate.copy(
                registrationStatus = newRegistrationStatus,
                expired = newExpiredDate,
                updated = LocalDateTime.now(),
                updatedBy = REGISTER,
                updatedByUser = authentication.name,
            )

        return saveAndCreateEventIfNotDraftAndApproved(updatedProduct, isUpdate = true)
    }

    suspend fun setDeletedStatus(
        ids: List<UUID>,
        authentication: Authentication,
    ): List<ProductRegistration> {
        val products =
            findByIdIn(ids).onEach {
                if (authentication.isSupplier() && it.supplierId != authentication.supplierId()) {
                    throw BadRequestException("product belongs to another supplier", type = ErrorType.UNAUTHORIZED)
                }
            }

        val productsToDelete =
            products.map {
                it.copy(
                    registrationStatus = RegistrationStatus.DELETED,
                    expired = LocalDateTime.now(),
                    updatedByUser = authentication.name,
                    updatedBy = REGISTER,
                )
            }

        return saveAllAndCreateEventIfNotDraftAndApproved(productsToDelete, isUpdate = true)
    }

    suspend fun deleteDraftVariants(
        ids: List<UUID>,
        authentication: Authentication,
    ) {
        val products =
            findByIdIn(ids).onEach {
                if (authentication.isSupplier() && it.supplierId != authentication.supplierId()) {
                    throw BadRequestException("product belongs to another supplier", type = ErrorType.UNAUTHORIZED)
                }
                if (!(it.draftStatus == DraftStatus.DRAFT && it.published == null)) {
                    throw BadRequestException("product is not draft")
                }
            }

        products.forEach {
            LOG.info("Delete called for id ${it.id} and supplierRef ${it.supplierRef}")
        }

        deleteAll(products)
    }

    suspend fun findAccessoryOrSparePartCombatibleWithSeriesId(seriesUUID: UUID) =
        productRegistrationRepository.findAccessoryOrSparePartCombatibleWithSeriesId(seriesUUID)

    suspend fun findAccessoryOrSparePartCombatibleWithSeriesIdAndSupplierId(seriesUUID: UUID, supplierId: UUID) =
        productRegistrationRepository.findAccessoryOrSparePartCombatibleWithSeriesIdAndSupplierId(seriesUUID, supplierId)

    suspend fun findByTechLabelValues(
        key: String? = null,
        unit: String? = null,
        value: String? = null
    ): List<ProductRegistration> {
        if (key.isNullOrBlank() && unit.isNullOrBlank() && value.isNullOrBlank()) {
            throw BadRequestException("At least one of key, unit or value must be provided")
        }
        val jsonQuery = StringBuilder("[{")
        if (!key.isNullOrBlank()) {
            jsonQuery.append("\"key\": \"$key\",")
        }
        if (!unit.isNullOrBlank()) {
            jsonQuery.append("\"unit\": \"$unit\",")
        }
        if (!value.isNullOrBlank()) {
            jsonQuery.append("\"value\": \"$value\",")
        }
        if (jsonQuery.endsWith(",")) jsonQuery.setLength(jsonQuery.length - 1) // Remove trailing comma
        jsonQuery.append("}]")
        LOG.info("Executing jsonQuery ${jsonQuery.toString()}")
        return productRegistrationRepository.findDistinctByProductTechDataJsonQuery(jsonQuery.toString())
    }

}

suspend fun <T : Any, R : Any> Page<T>.mapSuspend(transform: suspend (T) -> R): Page<R> {
    val content = this.content.map { transform(it) }
    return Page.of(content, this.pageable, this.totalSize)
}
