package no.nav.hm.grunndata.register.product

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Context
import io.micronaut.core.annotation.Order
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.register.archive.Archive
import no.nav.hm.grunndata.register.archive.ArchiveHandler
import no.nav.hm.grunndata.register.archive.ArchiveType

@Context
@Order(1)
class ProductArchiveHandler(
    private val objectMapper: ObjectMapper,
    private val productRegistrationRepository: ProductRegistrationRepository,
) : ArchiveHandler {


    override fun getArchiveType(): ArchiveType = ArchiveType.PRODUCT

    override suspend fun archive(): List<Archive> {
        val toBeDeleted = productRegistrationRepository.findByRegistrationStatus(RegistrationStatus.DELETED)
        LOG.info("${toBeDeleted.size} to be archived with status DELETED")
        val archives = toBeDeleted.map { toArchive ->
            productRegistrationRepository.deleteById(toArchive.id)
            Archive(
                oid = toArchive.id,
                type = ArchiveType.PRODUCT,
                keywords = "${toArchive.hmsArtNr}",
                payload = objectMapper.writeValueAsString(toArchive),
                archivedByUser = toArchive.updatedByUser
            )
        }
        return archives
    }

    override suspend fun unArchive(unArchive: Archive) {
        LOG.info("Unarchiving ProductRegistration with oid: ${unArchive.oid} and keywords: ${unArchive.keywords}")
        val productRegistration = objectMapper.readValue(unArchive.payload, ProductRegistration::class.java)
        val unarchived = productRegistrationRepository.findBySupplierRefAndSupplierId(
            productRegistration.supplierRef,
            productRegistration.supplierId
        )?.let {
            LOG.info("Found existing ProductRegistration with id: ${it.supplierRef} and supplierId: ${it.supplierId}, skipping it")
            it
        } ?: productRegistrationRepository.save(productRegistration.copy(registrationStatus = RegistrationStatus.INACTIVE))
    }

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(ProductArchiveHandler::class.java)
    }

}