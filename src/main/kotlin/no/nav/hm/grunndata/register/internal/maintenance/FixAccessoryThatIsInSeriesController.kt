package no.nav.hm.grunndata.register.internal.maintenance

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.register.product.ProductIdSeriesUUID
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.series.SeriesRegistrationService
import java.time.LocalDateTime
import java.util.UUID

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/fix/accessory-in-series")
@Hidden
open class FixAccessoryThatIsInSeriesController(
    private val productRegistrationRepository: ProductRegistrationRepository,
    private val productRegistrationService: ProductRegistrationService,
    private val seriesRegistrationService: SeriesRegistrationService
) {

    @Post("/")
    suspend fun fixAccessoryThatIsInSeries() {
        val ids = productRegistrationRepository.findNotMainProductsThatIsInSeries()
        LOG.info("Got ${ids.size} products that is not main product but is in series")
        ids.forEach {
            splitSries(it)
        }
    }

    @Transactional
    open suspend fun splitSries(id: ProductIdSeriesUUID) {
        val oldSeries = seriesRegistrationService.findById(id.seriesUUID)!!
        val products = productRegistrationRepository.findAllBySeriesUUID(id.seriesUUID)
        products.forEach { product ->
            val newsSeriesUUID = UUID.randomUUID()
            val newSeries = oldSeries.copy(
                id = newsSeriesUUID,
                identifier = newsSeriesUUID.toString(),
                title = product.articleName,
                published = product.published,
                expired = product.expired ?: LocalDateTime.now().plusYears(10),
                adminStatus = AdminStatus.APPROVED
            )
            val productInNewSerie = product.copy(
                seriesUUID = newsSeriesUUID,
                adminStatus = AdminStatus.APPROVED,
                updated = LocalDateTime.now(),
                title = product.articleName
            )
            seriesRegistrationService.saveAndCreateEventIfNotDraftAndApproved(newSeries, isUpdate = false)
            productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(productInNewSerie, isUpdate = true)
        }
    }

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(FixAccessoryThatIsInSeriesController::class.java)
    }

}
