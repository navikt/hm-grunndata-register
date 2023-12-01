package no.nav.hm.grunndata.register.product

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.security.authentication.Authentication
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.rapid.dto.*
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.event.EventItem
import no.nav.hm.grunndata.register.event.EventItemService
import no.nav.hm.grunndata.register.event.EventItemType
import no.nav.hm.grunndata.register.series.SeriesRegistrationDTO
import no.nav.hm.grunndata.register.series.SeriesRegistrationService
import no.nav.hm.grunndata.register.techlabel.TechLabelService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

@Singleton
open class ProductRegistrationService(private val productRegistrationRepository: ProductRegistrationRepository,
                                      private val productRegistrationHandler: ProductRegistrationHandler,
                                      private val techLabelService: TechLabelService,
                                      private val seriesRegistrationService: SeriesRegistrationService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(ProductRegistration::class.java)
    }

    open suspend fun findById(id: UUID) = productRegistrationRepository.findById(id)?.toDTO()

    open suspend fun save(dto: ProductRegistrationDTO) = productRegistrationRepository.save(dto.toEntity()).toDTO()

    open suspend fun update(dto: ProductRegistrationDTO) = productRegistrationRepository.update(dto.toEntity()).toDTO()

    open suspend fun findAll(spec: PredicateSpecification<ProductRegistration>?, pageable: Pageable): Page<ProductRegistrationDTO> =
        productRegistrationRepository.findAll(spec, pageable).map { it.toDTO() }


    open suspend fun findBySupplierRefAndSupplierId(supplierRef: String, supplierId: UUID) =
        productRegistrationRepository.findBySupplierRefAndSupplierId(supplierRef, supplierId)?.toDTO()

    open suspend fun findByIdAndSupplierId(id: UUID, supplierId: UUID) =
        productRegistrationRepository.findByIdAndSupplierId(id, supplierId)?.toDTO()


    @Transactional
    open suspend fun saveAndCreateEventIfNotDraftAndApproved(dto: ProductRegistrationDTO, isUpdate: Boolean): ProductRegistrationDTO {
        val saved = if (isUpdate) update(dto) else save(dto)
        productRegistrationHandler.queueDTORapidEvent(saved)
        return saved
    }

    suspend fun findBySeriesId(seriesId: String) = productRegistrationRepository.findBySeriesId(seriesId).map { it.toDTO() }

    suspend fun findBySeriesIdAndSupplierId(seriesId: String, supplierId: UUID) = productRegistrationRepository.findBySeriesIdAndSupplierId(seriesId, supplierId).map { it.toDTO() }
    suspend fun findSeriesGroup(supplierId: UUID, pageable: Pageable) = productRegistrationRepository.findSeriesGroup(supplierId, pageable)
    suspend fun findSeriesGroup(pageable: Pageable) = productRegistrationRepository.findSeriesGroup(pageable)
    open suspend fun createProductVariant(id: UUID, dto: DraftVariantDTO, authentication: Authentication) =
        findById(id)?.let {
            val productId = UUID.randomUUID()
            save(it.copy(
                supplierRef = dto.supplierRef,
                articleName = dto.articleName,
                id = productId,
                hmsArtNr = null,
                draftStatus =  DraftStatus.DRAFT,
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
            ))
        }


    open suspend fun createDraft(supplierId: UUID, authentication: Authentication,
                                 isAccessory:Boolean, isSparePart: Boolean): ProductRegistrationDTO {
        val productId = UUID.randomUUID()
        val product = ProductData (
            accessory = isAccessory,
            sparePart = isSparePart,
            attributes = Attributes (
                shortdescription = "",
                text = "en lang beskrivelse"
            )
        )
        val registration = ProductRegistrationDTO(
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
            version = 0)
        val draft = save(registration)
        LOG.info("Draft was created ${draft.id} by $supplierId")
        return draft
    }

    open suspend fun createDraftWith(supplierId: UUID, authentication: Authentication, isAccessory: Boolean,
                                     isSparePart: Boolean, draftWithDTO: ProductDraftWithDTO): ProductRegistrationDTO {
        val productId = UUID.randomUUID()
        val product = ProductData (
            accessory = isAccessory,
            sparePart = isSparePart,
            techData = createTechDataDraft(draftWithDTO),
            attributes = Attributes (
                shortdescription = "",
                text = draftWithDTO.text
            )
        )
        val registration = ProductRegistrationDTO(
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
            version = 0)
        val draft = save(registration)
        LOG.info("Draft was created ${draft.id} by $supplierId")
        return draft
    }

    private fun createTechDataDraft(draftWithDTO: ProductDraftWithDTO): List<TechData> =
        techLabelService.fetchLabelsByIsoCode(draftWithDTO.isoCategory)?.map {
            TechData(key = it.label, value = "", unit = it.unit ?:"")
        }?: emptyList()
}