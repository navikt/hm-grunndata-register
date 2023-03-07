package no.nav.hm.grunndata.register.product

import no.nav.hm.grunndata.rapid.dto.ProductRegistrationDTO


fun ProductRegistrationDTO.toEntity(): ProductRegistration = ProductRegistration(id = id,
    supplierId = supplierId, supplierRef =supplierRef, hmsArtNr = hmsArtNr, title = title, draftStatus = draftStatus,
    adminStatus = adminStatus, status = status, message = message, adminInfo = adminInfo, created = created, updated = updated,
    published = published, expired = expired, updatedByUser = updatedByUser, createdByUser = createdByUser,  createdBy = createdBy, updatedBy = updatedBy,
    createdByAdmin = createdByAdmin, productDTO = productDTO, version = version
)

 fun ProductRegistration.toDTO(): ProductRegistrationDTO = ProductRegistrationDTO(
    id = id, supplierId= supplierId, supplierRef =supplierRef, hmsArtNr = hmsArtNr, title = title, draftStatus = draftStatus,
    adminStatus = adminStatus, status = status,  message = message, adminInfo = adminInfo, created = created, updated = updated,
    published = published, expired = expired, updatedByUser = updatedByUser, createdByUser = createdByUser,
    createdBy = createdBy, updatedBy = updatedBy, createdByAdmin = createdByAdmin, productDTO = productDTO, version = version
)
