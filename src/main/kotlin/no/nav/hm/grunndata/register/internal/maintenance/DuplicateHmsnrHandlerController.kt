package no.nav.hm.grunndata.register.internal.maintenance

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.register.product.ProductIdHmsArtNr
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import java.time.LocalDateTime

@Hidden
@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/duplicate/hmsnr")
class DuplicateHmsnrHandlerController(private val productRegistrationRepository: ProductRegistrationRepository,
                                      private val productRegistrationService: ProductRegistrationService) {

    @Get("/")
    suspend fun findDuplicateHmsnr(): List<ProductIdHmsArtNr> {
        val duplicates = productRegistrationRepository.findAllDuplicateHmsArtnr()
        LOG.info("Found ${duplicates.size} duplicates")
        return duplicates
    }

    @Delete("/delete")
    suspend fun findAndDeleteDuplicateHmsnr(): List<ProductIdHmsArtNr> {
        val duplicates = productRegistrationRepository.findAllDuplicateHmsArtnr()
        duplicates.forEach { duplicate ->
            if (duplicate.hmsArtNr.isNotEmpty()) {
                productRegistrationService.findById(duplicate.id)?.let { product ->
                    LOG.info("Found product with id ${product.id}, hmsArtNr ${product.hmsArtNr}, to mark delete")
                    productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                        product.copy(
                            registrationStatus = RegistrationStatus.DELETED, updated = LocalDateTime.now(),
                            updatedByUser = "DuplicateHmsnr"
                        ), isUpdate = true
                    )

                }
            }
        }
        return duplicates
    }
    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(DuplicateHmsnrHandlerController::class.java)
    }
}