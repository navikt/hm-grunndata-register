package no.nav.hm.grunndata.register.productagreement

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Context
import io.micronaut.core.annotation.Order
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.register.archive.Archive
import no.nav.hm.grunndata.register.archive.ArchiveHandler
import no.nav.hm.grunndata.register.archive.ArchiveType

@Context
@Order(2)
class ProductAgreementArchiveHandler(private val productAgreementRegistrationRepository: ProductAgreementRegistrationRepository,
                                     private val objectMapper: ObjectMapper): ArchiveHandler {
    override fun getArchiveType(): ArchiveType = ArchiveType.PRODUCTAGREEMENT

    override suspend fun archive(): List<Archive> {
        val toBeDeleted = productAgreementRegistrationRepository.findByStatus(ProductAgreementStatus.DELETED)
        LOG.debug("${toBeDeleted.size} product agreements to be archived with status DELETED")
        val archives = toBeDeleted.map { pag ->
            val archive = Archive (
                oid = pag.productId!!,
                type = getArchiveType(),
                keywords = "${pag.supplierRef}, ${pag.hmsArtNr}",
                payload = objectMapper.writeValueAsString(pag),
                archivedByUser = pag.updatedBy
            )
            productAgreementRegistrationRepository.deleteById(pag.id)
            archive
        }
        return archives
    }

    override suspend fun unArchive(unarchive: Archive) {
        LOG.debug(
            "Unarchiving ProductAgreementRegistration with oid: {} and keywords: {}",
            unarchive.oid,
            unarchive.keywords
        )
        val productAgreement = objectMapper.readValue(unarchive.payload, ProductAgreementRegistration::class.java)
        productAgreementRegistrationRepository.save(productAgreement.copy(status = ProductAgreementStatus.INACTIVE))
    }

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(ProductAgreementArchiveHandler::class.java)
    }

}