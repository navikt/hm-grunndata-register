package no.nav.hm.grunndata.register.product

import io.micronaut.security.authentication.Authentication
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.*
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.RegisterRapidPushService
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierRepository
import no.nav.hm.grunndata.register.supplier.SupplierService
import no.nav.hm.grunndata.register.supplier.toDTO
import java.time.LocalDateTime
import java.util.*

@Singleton
class ProductRegistrationHandler(private val registerRapidPushService: RegisterRapidPushService,
                                 private val supplierService: SupplierService) {
    fun pushToRapidIfNotDraft(dto: ProductRegistrationDTO) {
        runBlocking {
            if (dto.draftStatus == DraftStatus.DONE) {
                val rapidDTO = dto.toRapidDTO()
                registerRapidPushService.pushDTOToKafka(rapidDTO, EventName.registeredProductV1)
            }
        }
    }

    private suspend fun ProductRegistrationDTO.toRapidDTO() = ProductRegistrationRapidDTO (
        id = id,
        draftStatus = draftStatus,
        adminStatus = adminStatus,
        registrationStatus = registrationStatus,
        message = message,
        created = created,
        updated = updated,
        published = published,
        expired = expired,
        createdBy = createdBy,
        updatedBy = updatedBy,
        createdByAdmin = createdByAdmin,
        version = version,
        productDTO = productData.toProductDTO(this),
    )

    private suspend fun ProductData.toProductDTO(registration: ProductRegistrationDTO): ProductRapidDTO = ProductRapidDTO (
        id = registration.id,
        supplier = supplierService.findById(registration.supplierId)!!.toDTO(),
        supplierRef = registration.supplierRef,
        title =  registration.title,
        articleName = registration.articleName,
        hmsArtNr = registration.hmsArtNr,
        identifier = registration.id.toString(),
        isoCategory = isoCategory,
        accessory = accessory,
        sparePart = sparePart,
        seriesId = seriesId,
        techData = techData,
        media = media,
        created = registration.created,
        updated = registration.updated,
        published = registration.published ?: LocalDateTime.now(),
        expired = registration.expired ?: LocalDateTime.now().plusYears(10),
        agreementInfo = agreementInfo,
        agreements = agreements,
        hasAgreement = agreementInfo!=null,
        createdBy = registration.createdBy,
        updatedBy = registration.updatedBy,
        attributes = attributes,
        status = setCorrectStatusFor(registration)
    )

    private fun setCorrectStatusFor(registration: ProductRegistrationDTO): ProductStatus =
        if (registration.registrationStatus == RegistrationStatus.DELETED
                ||  registration.adminStatus != AdminStatus.APPROVED
                ||  registration.draftStatus == DraftStatus.DRAFT
                ||  LocalDateTime.now().isAfter(registration.expired))
            ProductStatus.INACTIVE
        else
            ProductStatus.ACTIVE

    fun createProductVariant(registration: ProductRegistration, supplierRef: String, authentication: Authentication): ProductRegistrationDTO {
        val productId = UUID.randomUUID()
        return registration.toDTO().copy(
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

}
fun Authentication.isAdmin(): Boolean  = roles.contains(Roles.ROLE_ADMIN)

fun ProductRegistrationDTO.toEntity(): ProductRegistration = ProductRegistration(
    id = id, supplierId = supplierId, supplierRef = supplierRef, hmsArtNr = hmsArtNr, title = title,
    articleName = articleName, draftStatus = draftStatus, adminStatus = adminStatus,
    registrationStatus = registrationStatus, message = message, adminInfo = adminInfo, created = created,
    updated = updated, published = published, expired = expired, updatedByUser = updatedByUser,
    createdByUser = createdByUser, createdBy = createdBy, updatedBy = updatedBy, createdByAdmin = createdByAdmin,
    productData = productData, version = version
)

fun ProductRegistration.toDTO(): ProductRegistrationDTO = ProductRegistrationDTO(
    id = id, supplierId = supplierId, supplierRef = supplierRef, hmsArtNr = hmsArtNr, title = title,
    articleName = articleName, draftStatus = draftStatus, adminStatus = adminStatus,
    registrationStatus = registrationStatus, message = message, adminInfo = adminInfo, created = created,
    updated = updated, published = published, expired = expired, updatedByUser = updatedByUser,
    createdByUser = createdByUser, createdBy = createdBy, updatedBy = updatedBy, createdByAdmin = createdByAdmin,
    productData = productData, version = version
)