package no.nav.hm.grunndata.register.product

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.data.runtime.criteria.where
import io.micronaut.security.authentication.Authentication
import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import java.time.LocalDateTime
import java.util.*
import javax.transaction.Transactional

@Singleton
class ProductRegistrationService(private val productRegistrationRepository: ProductRegistrationRepository,
                                 private val productRegistrationHandler: ProductRegistrationHandler) {


    open suspend fun findById(id: UUID) = productRegistrationRepository.findById(id)?.toDTO()

    open suspend fun save(dto: ProductRegistrationDTO) = productRegistrationRepository.save(dto.toEntity()).toDTO()

    open suspend fun update(dto: ProductRegistrationDTO) = productRegistrationRepository.update(dto.toEntity()).toDTO()

    open suspend fun findAll(params: HashMap<String,String>?, pageable: Pageable): Page<ProductRegistrationDTO> =
        productRegistrationRepository.findAll(buildCriteriaSpec(params), pageable).map { it.toDTO() }


    open suspend fun findBySupplierIdAndSupplierRef(supplierId: UUID, supplierRef: String) =
        productRegistrationRepository.findBySupplierIdAndSupplierRef(supplierId, supplierRef)?.toDTO()

    @Transactional
    open suspend fun saveAndPushToKafka(dto: ProductRegistrationDTO, isUpdate: Boolean): ProductRegistrationDTO {

        val saved = if (isUpdate) update(dto) else save(dto)
        productRegistrationHandler.pushToRapidIfNotDraft(saved)
        return saved
    }

    private fun buildCriteriaSpec(params: HashMap<String, String>?): PredicateSpecification<ProductRegistration>?
            = params?.let {
        where {
            if (params.contains("supplierRef")) root[ProductRegistration::supplierRef] eq params["supplierRef"]
            if (params.contains("hmsArtNr")) root[ProductRegistration::hmsArtNr] eq params["hmsArtNr"]
            if (params.contains("adminStatus")) root[ProductRegistration::adminStatus] eq AdminStatus.valueOf(params["adminStatus"]!!)
            if (params.contains("supplierId"))  root[ProductRegistration::supplierId] eq UUID.fromString(params["supplierId"]!!)
            if (params.contains("draft")) root[ProductRegistration::draftStatus] eq DraftStatus.valueOf(params["draft"]!!)
            if (params.contains("createdByUser")) root[ProductRegistration::createdByUser] eq params["createdByUser"]
            if (params.contains("updatedByUser")) root[ProductRegistration::updatedByUser] eq params["updatedByUser"]
            if (params.contains("title")) criteriaBuilder.like(root[ProductRegistration::title], params["title"])
        }
    }

    private fun createProductVariant(registration: ProductRegistrationDTO, supplierRef: String, authentication: Authentication): ProductRegistrationDTO {
        val productId = UUID.randomUUID()
        return registration.copy(
            supplierRef = supplierRef,
            id = productId,
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
        )
    }

    suspend fun createProductVariant(id: UUID, supplierRef: String, authentication: Authentication) =
        findById(id)?.let { createProductVariant(it, supplierRef, authentication) }


}