package no.nav.hm.grunndata.register.catalog

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
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
        return agreementAction == other.agreementAction &&
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
                mainProduct == other.mainProduct &&
                sparePart == other.sparePart &&
                accessory == other.accessory &&
                agreementId == other.agreementId &&
                supplierId == other.supplierId

    }
}
