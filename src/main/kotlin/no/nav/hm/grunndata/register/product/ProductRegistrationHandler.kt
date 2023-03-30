package no.nav.hm.grunndata.register.product

import io.micronaut.security.authentication.Authentication
import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.*
import java.time.LocalDateTime
import java.util.*

@Singleton
class ProductRegistrationHandler {

    fun makeTemplateOf(registration: ProductRegistration, authentication: Authentication): ProductRegistrationDTO {
        val productId = UUID.randomUUID()
        val product = registration.toDTO().productDTO.copy(
            id = productId,
            status = ProductStatus.INACTIVE,
            hmsArtNr=null,
            identifier = productId.toString(),
            supplierRef = productId.toString(),
            created = LocalDateTime.now(),
            updated = LocalDateTime.now(),
            published = LocalDateTime.now(),
            expired = LocalDateTime.now().plusYears(10) ,
            createdBy = "REGISTER",
            updatedBy = "REGISTER"
        )
        return registration.toDTO().copy(
            id = productId,
            draftStatus =  DraftStatus.DRAFT,
            adminStatus = AdminStatus.PENDING,
            message = null,
            adminInfo = null,
            created = LocalDateTime.now(),
            updated = LocalDateTime.now(),
            published = LocalDateTime.now(),
            expired = LocalDateTime.now().plusYears(10),
            updatedByUser = authentication.name,
            createdByUser = authentication.name,
            createdBy = product.createdBy,
            updatedBy = product.updatedBy,
            createdByAdmin = authentication.isAdmin(),
            productDTO = product
        )
    }

}
