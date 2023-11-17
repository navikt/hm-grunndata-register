package no.nav.hm.grunndata.register.product

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.security.authentication.Authentication
import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.*
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.event.EventItem
import no.nav.hm.grunndata.register.event.EventItemService
import no.nav.hm.grunndata.register.event.EventItemType
import no.nav.hm.grunndata.register.event.RegisterRapidPushService
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.grunndata.register.supplier.toRapidDTO
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Singleton
class ProductRegistrationHandler(private val registerRapidPushService: RegisterRapidPushService,
                                 private val supplierRegistrationService: SupplierRegistrationService,
                                 private val objectMapper: ObjectMapper,
                                 private val eventItemService: EventItemService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(ProductRegistrationHandler::class.java)
    }

    suspend fun sendRapidEvent(eventItem: EventItem) {
        val dto = objectMapper.readValue(eventItem.payload, ProductRegistrationDTO::class.java)
        registerRapidPushService.pushToRapid(dto.toRapidDTO(), eventItem)
    }

    suspend fun queueDTORapidEvent(dto: ProductRegistrationDTO,
                                   eventName: String = EventName.registeredProductV1,
                                   extraKeyValues:Map<String, Any> = emptyMap()) {
        if (dto.draftStatus == DraftStatus.DONE && dto.adminStatus == AdminStatus.APPROVED) {
            LOG.info("queueDTORapidEvent for ${dto.id} with adminstatus ${dto.adminStatus} ")
            eventItemService.createNewEventItem(
                type = EventItemType.PRODUCT,
                oid = dto.id,
                byUser = dto.updatedByUser,
                eventName = eventName,
                payload = dto,
                extraKeyValues = extraKeyValues
            )
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
        supplier = supplierRegistrationService.findById(registration.supplierId)!!.toRapidDTO(),
        supplierRef = registration.supplierRef,
        title =  registration.title,
        articleName = registration.articleName,
        hmsArtNr = registration.hmsArtNr,
        identifier = registration.id.toString(),
        isoCategory = registration.isoCategory,
        accessory = accessory,
        sparePart = sparePart,
        seriesUUID = registration.seriesUUID,
        seriesId = registration.seriesId,
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

}
fun Authentication.isAdmin(): Boolean  = roles.contains(Roles.ROLE_ADMIN)

fun ProductRegistrationDTO.toEntity(): ProductRegistration = ProductRegistration(
    id = id, supplierId = supplierId, seriesId = seriesId, seriesUUID = seriesUUID, supplierRef = supplierRef, hmsArtNr = hmsArtNr, title = title,
    articleName = articleName, draftStatus = draftStatus, adminStatus = adminStatus,
    registrationStatus = registrationStatus, message = message, adminInfo = adminInfo, created = created,
    updated = updated, published = published, expired = expired, updatedByUser = updatedByUser,
    createdByUser = createdByUser, createdBy = createdBy, updatedBy = updatedBy, createdByAdmin = createdByAdmin,
    productData = productData, isoCategory = isoCategory, version = version
)

fun ProductRegistration.toDTO(): ProductRegistrationDTO = ProductRegistrationDTO(
    id = id, supplierId = supplierId, seriesId = seriesId, seriesUUID = seriesUUID, supplierRef = supplierRef, hmsArtNr = hmsArtNr, title = title,
    articleName = articleName, draftStatus = draftStatus, adminStatus = adminStatus,
    registrationStatus = registrationStatus, message = message, adminInfo = adminInfo, created = created,
    updated = updated, published = published, expired = expired, updatedByUser = updatedByUser,
    createdByUser = createdByUser, createdBy = createdBy, updatedBy = updatedBy, createdByAdmin = createdByAdmin,
    productData = productData, isoCategory = isoCategory, version = version
)
