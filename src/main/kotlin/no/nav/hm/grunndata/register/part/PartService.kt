package no.nav.hm.grunndata.register.part

import io.micronaut.security.authentication.Authentication
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.series.SeriesRegistrationService
import org.slf4j.LoggerFactory

@Singleton
open class PartService(
    private val seriesRegistrationService: SeriesRegistrationService,
    private val productRegistrationService: ProductRegistrationService
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(PartService::class.java)
    }

    @Transactional
    open suspend fun createDraftWith(
        authentication: Authentication,
        draftWith: PartDraftWithDTO,
    ): ProductRegistration {

        val series = draftWith.toSeriesRegistration(authentication).also { seriesRegistration ->
            seriesRegistrationService.save(
                seriesRegistration,
            )
        }

        val product = draftWith.toProductRegistration(series.id).also { productRegistration ->
            productRegistrationService.save(
                productRegistration,
            )
        }

        LOG.info("create part draft for new series: ${series.id} and product: ${product.id}")

        return product

    }


}