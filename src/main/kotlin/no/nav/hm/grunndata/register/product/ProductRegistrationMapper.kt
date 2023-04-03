package no.nav.hm.grunndata.register.product

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
