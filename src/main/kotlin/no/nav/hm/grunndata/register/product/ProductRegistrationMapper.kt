package no.nav.hm.grunndata.register.product

import no.nav.hm.grunndata.rapid.dto.ProductRegistrationDTO


fun ProductRegistrationDTO.toEntity(): ProductRegistration = ProductRegistration(id = id,
    supplierId = productDTO.supplier.id, supplierRef = productDTO.supplierRef, hmsArtNr = productDTO.hmsArtNr,
    title = productDTO.title, articleName = productDTO.articleName, draftStatus = draftStatus,
    adminStatus = adminStatus, registrationStatus = registrationStatus, message = message, adminInfo = adminInfo, created = created,
    updated = updated, published = published, expired = expired, updatedByUser = updatedByUser,
    createdByUser = createdByUser, createdBy = createdBy, updatedBy = updatedBy, createdByAdmin = createdByAdmin,
    productDTO = productDTO, version = version
)

 fun ProductRegistration.toDTO(): ProductRegistrationDTO = ProductRegistrationDTO(
     id = id, draftStatus = draftStatus, adminStatus = adminStatus, registrationStatus = registrationStatus,
     message = message, adminInfo = adminInfo, created = created, updated = updated, published = published,
     expired = expired, updatedByUser = updatedByUser, createdByUser = createdByUser, createdBy = createdBy,
     updatedBy = updatedBy, createdByAdmin = createdByAdmin, productDTO = productDTO, version = version
)
