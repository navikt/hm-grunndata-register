package no.nav.hm.grunndata.register.catalog

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import java.time.LocalDateTime
import java.util.UUID

@MappedEntity("catalog_import_v1")
data class CatalogImport(
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val agreementAction: String, //oebs catalog action
    val orderRef: String, // oebs unique order reference for agreement
    val hmsArtNr: String,
    val iso: String, // oebs iso category
    val title: String, // oebs article description
    val supplierRef: String,
    val reference: String, // agreement reference
    val postNr: String?,
    val dateFrom: String,
    val dateTo: String,
    val articleAction: String,
    val articleType: String,
    val functionalChange: String,
    val forChildren: String,
    val supplierName: String,
    val supplierCity: String,
    val mainProduct: Boolean,
    val sparePart: Boolean,
    val accessory: Boolean,
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
)
