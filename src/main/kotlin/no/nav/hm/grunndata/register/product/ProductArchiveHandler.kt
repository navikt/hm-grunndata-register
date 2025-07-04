package no.nav.hm.grunndata.register.product

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.archive.Archive
import no.nav.hm.grunndata.register.archive.ArchiveHandler
import no.nav.hm.grunndata.register.archive.ArchiveType

@Singleton
class ProductArchiveHandler(private val objectMapper: ObjectMapper, private val productRegistrationRepository: ProductRegistrationRepository): ArchiveHandler<ProductRegistration> {

    override fun getArchivePayloadClass(): Class<out ProductRegistration> =  ProductRegistration::class.java

    override fun toArchive(toArchive: ProductRegistration): Archive = Archive (
        oid = toArchive.id,
        type = ArchiveType.PRODUCT,
        keywords = "${toArchive.hmsArtNr} ${toArchive.articleName}",
        payload = objectMapper.writeValueAsString(toArchive),
        archivedByUser = toArchive.updatedByUser
    )

    override fun unArchive(archive: Archive): ProductRegistration {
        TODO("Not yet implemented")
    }
}