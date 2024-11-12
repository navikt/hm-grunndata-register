package no.nav.hm.grunndata.register.version

import io.micronaut.data.model.Pageable
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.product.version.ProductRegistrationVersion
import no.nav.hm.grunndata.register.product.version.ProductRegistrationVersionService
import no.nav.hm.grunndata.register.series.SeriesRegistrationDTO
import no.nav.hm.grunndata.register.series.SeriesRegistrationService
import no.nav.hm.grunndata.register.series.version.SeriesRegistrationVersion
import no.nav.hm.grunndata.register.series.version.SeriesRegistrationVersionService
import no.nav.hm.grunndata.register.series.toEntity
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import org.slf4j.LoggerFactory

@Singleton
open class CreateBaseVersionHandler(
    private val supplierRegistrationService: SupplierRegistrationService,
    private val productRegistrationService: ProductRegistrationService,
    private val seriesRegistrationService: SeriesRegistrationService,
    private val seriesRegistrationVersionService: SeriesRegistrationVersionService,
    private val productRegistrationVersionService: ProductRegistrationVersionService,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(CreateBaseVersionHandler::class.java)

        // HMDB-5205 = Cognita
        private val suppliersInRegister: Set<String> = setOf("HMDB-5205")
    }

    @Transactional
    open suspend fun createVersionsWhereMissingForMigratedSuppliers() {
        val allSuppliers = supplierRegistrationService.findAll(null, Pageable.UNPAGED).map { it.identifier }

        LOG.info("Creating base versions for migrated suppliers")
        allSuppliers.forEach {
            var countSeriesBaseVersionsCreated = 0
            var countProductVersionsCreated = 0

            val supplier = supplierRegistrationService.findByIdentifier(it)
            val seriesList =
                supplier?.let { s ->
                    seriesRegistrationService.findBySupplierId(s.id).filter { series ->
                        series.status != SeriesStatus.DELETED &&
                            series.published != null
                    }
                } ?: emptyList()

            seriesList.forEach { series ->
                val seriesVersion =
                    seriesRegistrationVersionService.findBySeriesIdAndVersion(series.id, series.version ?: 0)
                if (seriesVersion == null) {
                    seriesRegistrationVersionService.save(series.toVersion())
                    countSeriesBaseVersionsCreated++
                }
            }

            LOG.info("Created base version for $countSeriesBaseVersionsCreated series for supplier $it")

            val products =
                supplier?.let { s -> productRegistrationService.findBySupplierId(s.id) }?.filter { product ->
                    product.registrationStatus != RegistrationStatus.DELETED &&
                        product.published != null
                } ?: emptyList()

            products.forEach { product ->
                val productVersion =
                    productRegistrationVersionService.findByProductIdAndVersion(product.id, product.version ?: 0)
                if (productVersion == null) {
                    productRegistrationVersionService.save(product.toVersion())
                    countProductVersionsCreated++
                }
            }

            LOG.info("Created base version for $countProductVersionsCreated products for supplier $it")
        }
    }

    private fun SeriesRegistrationDTO.toVersion(): SeriesRegistrationVersion =
        SeriesRegistrationVersion(
            seriesId = this.id,
            version = this.version,
            draftStatus = this.draftStatus,
            adminStatus = this.adminStatus,
            status = this.status,
            updated = this.updated,
            seriesRegistration = this.toEntity(),
            updatedBy = this.updatedBy,
        )

    private fun ProductRegistration.toVersion(): ProductRegistrationVersion =
        ProductRegistrationVersion(
            productId = this.id,
            version = this.version,
            draftStatus = this.draftStatus,
            adminStatus = this.adminStatus,
            status = this.registrationStatus,
            updated = this.updated,
            productRegistration = this,
            updatedBy = this.updatedBy,
        )
}
