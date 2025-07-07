package no.nav.hm.grunndata.register.product

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.register.archive.Archive
import no.nav.hm.grunndata.register.archive.ArchiveHandler
import no.nav.hm.grunndata.register.archive.ArchiveType
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationRepository

@Singleton
class ProductArchiveHandler(
    private val objectMapper: ObjectMapper,
    private val productAgreementRegistrationRepository: ProductAgreementRegistrationRepository,
    private val productRegistrationRepository: ProductRegistrationRepository,
) : ArchiveHandler<ProductRegistration> {


    override fun getArchivePayloadClass(): Class<out ProductRegistration> = ProductRegistration::class.java

    override suspend fun archive(toArchive: ProductRegistration): List<Archive> {
        val pags = productAgreementRegistrationRepository.findByProductId(toArchive.id)
        val agreementArchives = pags.map {
            Archive(
                oid = it.productId!!,
                type = ArchiveType.PRODUCTAGREEMENT,
                keywords = "${it.hmsArtNr}",
                payload = objectMapper.writeValueAsString(it),
                archivedByUser = toArchive.updatedByUser
            )
        }
        val productArchive = Archive(
            oid = toArchive.id,
            type = ArchiveType.PRODUCT,
            keywords = "${toArchive.hmsArtNr}",
            payload = objectMapper.writeValueAsString(toArchive),
            archivedByUser = toArchive.updatedByUser
        )
        pags.forEach { productAgreementRegistrationRepository.deleteById(it.id) }
        productRegistrationRepository.deleteById(toArchive.id)
        return agreementArchives + productArchive
    }

    override suspend fun unArchive(archive: Archive): ProductRegistration {
        LOG.info("Unarchiving ProductRegistration with oid: ${archive.oid} and keywords: ${archive.keywords}")
        val productRegistration = objectMapper.readValue(archive.payload, ProductRegistration::class.java)
        val unarchived = productRegistrationRepository.findBySupplierRefAndSupplierId(
            productRegistration.supplierRef,
            productRegistration.supplierId
        )?.let {
            LOG.info("Found existing ProductRegistration with id: ${it.supplierRef} and supplierId: ${it.supplierId}, skipping it")
            it
        } ?: productRegistrationRepository.save(productRegistration)
        return unarchived
    }

    override suspend fun toBeArchived(): List<ProductRegistration> {
        LOG.info("Fetching ProductRegistrations to be archived")
        return productRegistrationRepository.findByRegistrationStatus(RegistrationStatus.DELETED)
            .onEach { LOG.info("Found ProductRegistration to archive: ${it.id} with hmsNr: ${it.hmsArtNr}") }
    }

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(ProductArchiveHandler::class.java)
    }

}