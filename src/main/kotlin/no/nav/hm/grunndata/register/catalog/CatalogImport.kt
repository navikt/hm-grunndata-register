package no.nav.hm.grunndata.register.catalog

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationDTO
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.hashCode

@MappedEntity("catalog_import_v1")
data class CatalogImport(
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val agreementAction: String, //oebs catalog action
    val orderRef: String, // oebs unique order reference for agreement
    val hmsArtNr: String,
    val iso: String, // oebs iso category
    val iso22: String, // oebs iso category 22
    val title: String, // oebs article description,
    val supplierId: UUID,
    val supplierRef: String,
    val reference: String, // agreement reference
    val postNr: String?,
    val dateFrom: LocalDate,
    val dateTo: LocalDate,
    val articleAction: String,
    val articleType: String,
    val functionalChange: String,
    val forChildren: String,
    val supplierName: String,
    val supplierCity: String,
    val mainProduct: Boolean,
    val sparePart: Boolean,
    val accessory: Boolean,
    val agreementId: UUID,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CatalogImport) return false
        return  mainProduct == other.mainProduct &&
                agreementAction == other.agreementAction &&
                orderRef == other.orderRef &&
                hmsArtNr == other.hmsArtNr &&
                iso == other.iso &&
                title == other.title &&
                supplierRef == other.supplierRef &&
                reference == other.reference &&
                postNr == other.postNr &&
                dateFrom == other.dateFrom &&
                dateTo == other.dateTo &&
                articleAction == other.articleAction &&
                articleType == other.articleType &&
                functionalChange == other.functionalChange &&
                forChildren == other.forChildren &&
                supplierName == other.supplierName &&
                supplierCity == other.supplierCity &&
                sparePart == other.sparePart &&
                accessory == other.accessory &&
                agreementId == other.agreementId &&
                supplierId == other.supplierId

    }

    override fun hashCode(): Int {
        var result = mainProduct.hashCode()
        result = 31 * result + sparePart.hashCode()
        result = 31 * result + accessory.hashCode()
        result = 31 * result + agreementAction.hashCode()
        result = 31 * result + orderRef.hashCode()
        result = 31 * result + hmsArtNr.hashCode()
        result = 31 * result + iso.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + supplierId.hashCode()
        result = 31 * result + supplierRef.hashCode()
        result = 31 * result + reference.hashCode()
        result = 31 * result + (postNr?.hashCode() ?: 0)
        result = 31 * result + dateFrom.hashCode()
        result = 31 * result + dateTo.hashCode()
        result = 31 * result + articleAction.hashCode()
        result = 31 * result + articleType.hashCode()
        result = 31 * result + functionalChange.hashCode()
        result = 31 * result + forChildren.hashCode()
        result = 31 * result + supplierName.hashCode()
        result = 31 * result + supplierCity.hashCode()
        result = 31 * result + agreementId.hashCode()
        return result
    }
}
