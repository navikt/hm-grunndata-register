package no.nav.hm.grunndata.register.product

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.series.SeriesDataDTO
import no.nav.hm.grunndata.register.series.SeriesRegistration
import no.nav.hm.grunndata.register.series.SeriesRegistrationRepository
import org.junit.jupiter.api.Test

@MicronautTest
class ProductPersistListenerTest(private val productRegistrationRepository: ProductRegistrationRepository,
                                 private val seriesRegistrationRepository: SeriesRegistrationRepository) {

    @Test
    fun testProductPersistListener() {
        val seriesId = UUID.randomUUID()
        val supplierId = UUID.randomUUID()
        val prod1 = ProductRegistration(
            seriesUUID = seriesId,
            draftStatus = DraftStatus.DRAFT,
            adminStatus = AdminStatus.PENDING,
            registrationStatus = RegistrationStatus.ACTIVE,
            articleName = "articleName",
            productData = ProductData(),
            supplierId = supplierId,
            hmsArtNr = "hmsArtNr123",
            id = UUID.randomUUID(),
            supplierRef = "supplierRef",
        )
        val prod2 = ProductRegistration(
            seriesUUID = seriesId,
            draftStatus = DraftStatus.DRAFT,
            adminStatus = AdminStatus.PENDING,
            registrationStatus = RegistrationStatus.ACTIVE,
            articleName = "articleName",
            productData = ProductData(),
            supplierId = supplierId,
            hmsArtNr = UUID.randomUUID().toString(),
            id = UUID.randomUUID(),
            supplierRef = "supplierRef2",
        )

        val series = SeriesRegistration(
            id = seriesId,
            supplierId = supplierId,
            identifier = UUID.randomUUID().toString(),
            title = "title-X",
            text = "text",
            isoCategory = "12345678",
            seriesData = SeriesDataDTO(),
            draftStatus = DraftStatus.DONE,
            status = SeriesStatus.ACTIVE,
            adminStatus = AdminStatus.APPROVED,
            created = LocalDateTime.now(),
            updated = LocalDateTime.now(),
            expired = LocalDateTime.now().plusYears(15),
            published = LocalDateTime.now(),
            createdBy = "createdBy",
            updatedBy = "updatedBy",
            updatedByUser = "updatedByUser",
            createdByUser = "createdByUser",
            createdByAdmin = false)

        runBlocking {
            val savedSeries = seriesRegistrationRepository.save(series)
            val saved1 = productRegistrationRepository.save(prod1)
            val saved2 = productRegistrationRepository.save(prod2)
            val update1 = productRegistrationRepository.update(
                saved1.copy(registrationStatus = RegistrationStatus.INACTIVE)
            )
            val update2 = productRegistrationRepository.update(
                saved2.copy(registrationStatus = RegistrationStatus.ACTIVE)
            )
            val foundSeries = seriesRegistrationRepository.findById(seriesId)
            foundSeries.shouldNotBeNull()
            foundSeries.status shouldBe SeriesStatus.ACTIVE
            val update3 = productRegistrationRepository.update(
                update2.copy(registrationStatus = RegistrationStatus.INACTIVE)
            )
            val inActiveSeries = seriesRegistrationRepository.findById(seriesId)
            inActiveSeries.shouldNotBeNull()
            inActiveSeries.status shouldBe SeriesStatus.INACTIVE
        }
    }
}