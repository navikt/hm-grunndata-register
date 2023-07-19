package no.nav.hm.grunndata.register.product

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.security.authentication.Authentication
import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import java.time.LocalDateTime
import java.util.*
import javax.transaction.Transactional

@Singleton
open class ProductRegistrationService(private val productRegistrationRepository: ProductRegistrationRepository,
                                 private val productRegistrationHandler: ProductRegistrationHandler) {


    open suspend fun findById(id: UUID) = productRegistrationRepository.findById(id)?.toDTO()

    open suspend fun save(dto: ProductRegistrationDTO) = productRegistrationRepository.save(dto.toEntity()).toDTO()

    open suspend fun update(dto: ProductRegistrationDTO) = productRegistrationRepository.update(dto.toEntity()).toDTO()

    open suspend fun findAll(spec: PredicateSpecification<ProductRegistration>?, pageable: Pageable): Page<ProductRegistrationDTO> =
        productRegistrationRepository.findAll(spec, pageable).map { it.toDTO() }


    open suspend fun findBySupplierIdAndSupplierRef(supplierId: UUID, supplierRef: String) =
        productRegistrationRepository.findBySupplierIdAndSupplierRef(supplierId, supplierRef)?.toDTO()

    open suspend fun findByIdAndSupplierId(id: UUID, supplierId: UUID) = productRegistrationRepository.findByIdAndSupplierId(id, supplierId)?.toDTO()

    @Transactional
    open suspend fun saveAndPushToKafka(dto: ProductRegistrationDTO, isUpdate: Boolean): ProductRegistrationDTO {

        val saved = if (isUpdate) update(dto) else save(dto)
        productRegistrationHandler.pushToRapidIfNotDraft(saved)
        return saved
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

    open suspend fun createProductVariant(id: UUID, supplierRef: String, authentication: Authentication) =
        findById(id)?.let { createProductVariant(it, supplierRef, authentication) }



}