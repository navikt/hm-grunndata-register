package no.nav.hm.grunndata.register.product.workswith

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.product.ProductData
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import no.nav.hm.grunndata.register.series.SeriesDataDTO
import no.nav.hm.grunndata.register.series.SeriesRegistration
import no.nav.hm.grunndata.register.series.SeriesRegistrationRepository
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

@MicronautTest
class WorksWithConnectorTest(private val worksWithConnector: WorksWithConnector,
                             private val productRegistrationRepository: ProductRegistrationRepository,
                             private val seriesRegistrationRepository: SeriesRegistrationRepository) {

    @Test
    fun `test works with connector`() {
        runBlocking {
            val sourceId = UUID.randomUUID()
            val sourceSeriesId = UUID.randomUUID()
            val targetId = UUID.randomUUID()
            val targetSeriesId = UUID.randomUUID()
            val supplierId = UUID.randomUUID()
            val source = productRegistrationRepository.save(
                ProductRegistration(
                    id = sourceId,
                    title = "Source product",
                    hmsArtNr = "333333",
                    seriesUUID = sourceSeriesId,
                    articleName = "Test article",
                    supplierId = supplierId,
                    supplierRef = "Source ref1",
                    productData = ProductData(),
                    accessory = false,
                    mainProduct = true,
                    created = LocalDateTime.now()
                )
            )
            val sourceSeries = seriesRegistrationRepository.save(
                SeriesRegistration(
                    id = sourceSeriesId,
                    supplierId = supplierId,
                    identifier = sourceSeriesId.toString(),
                    title = "Source Test series",
                    text = "Source Test series text",
                    isoCategory = "ISO1234",
                    seriesData = SeriesDataDTO()
                )
            )
            val target  = productRegistrationRepository.save(
                ProductRegistration(
                    id = targetId,
                    title = "Target product",
                    hmsArtNr = "222222",
                    seriesUUID = targetSeriesId,
                    articleName = "Test article",
                    supplierId = supplierId,
                    supplierRef = "Target ref1",
                    productData = ProductData(),
                    accessory = false,
                    mainProduct = true,
                    created = LocalDateTime.now()
                )
            )
            val targetSeries = seriesRegistrationRepository.save(
                SeriesRegistration(
                    id = targetSeriesId,
                    supplierId = supplierId,
                    identifier = targetSeriesId.toString(),
                    title = "Target Test series",
                    text = "Target Test series text",
                    isoCategory = "ISO1234",
                    seriesData = SeriesDataDTO()
                )
            )

            val result = worksWithConnector.addConnection(
                WorksWithMapping(
                    sourceProductId = sourceId,
                    targetProductId = targetId
                )
            )
            result.shouldNotBeNull()
            val worksWith = result.productData.attributes.worksWith
            worksWith.shouldNotBeNull()
            worksWith.productIds shouldContain targetId
            worksWith.seriesIds shouldContain targetSeriesId
            val reverse = productRegistrationRepository.findById(targetId)
            val reverseWorksWith = reverse?.productData?.attributes?.worksWith
            reverseWorksWith.shouldNotBeNull()
            reverseWorksWith.productIds shouldContain sourceId
            reverseWorksWith.seriesIds shouldContain sourceSeriesId

        }
    }
}