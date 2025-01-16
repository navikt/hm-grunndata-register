package no.nav.hm.grunndata.register.catalog

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.CatalogFileStatus


@MappedEntity("catalog_file_v1")
data class CatalogFile(
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val fileName: String,
    val fileSize: Long,
    val orderRef: String,
    @field:TypeDef(type = DataType.JSON)
    val catalogList: List<CatalogImportExcelDTO> = emptyList(),
    val supplierId: UUID,
    val updatedByUser: String,
    val created: LocalDateTime,
    val updated: LocalDateTime,
    val status: CatalogFileStatus = CatalogFileStatus.PENDING,
)

@Introspected
data class CatalogFileDTO(
    val id: UUID,
    val fileName: String,
    val fileSize: Long,
    val orderRef: String,
    val supplierId: UUID,
    val updatedByUser: String,
    val created: LocalDateTime,
    val updated: LocalDateTime,
    val status: CatalogFileStatus,
)


data class CatalogImportExcelDTO(
    val rammeavtaleHandling:String, // oebs operation for rammeavtale
    val bestillingsNr: String,
    val hmsArtNr: String,
    val iso: String,
    val title: String,
    val supplierRef: String,
    val reference: String,
    val delkontraktNr: String?,
    val dateFrom: String,
    val dateTo: String,
    val artikkelHandling: String, // oebs operation for artikkel
    val articleType: String,
    val funksjonsendring: String,
    val forChildren: String,
    val supplierName: String,
    val supplierCity: String,
    val mainProduct: Boolean,
    val sparePart: Boolean,
    val accessory: Boolean,
)


fun CatalogImportExcelDTO.toEntity() = CatalogImport(
    agreementAction = rammeavtaleHandling,
    orderRef = bestillingsNr,
    hmsArtNr = hmsArtNr,
    iso = iso,
    title = title,
    supplierRef = supplierRef,
    reference = reference,
    postNr = delkontraktNr,
    dateFrom = LocalDate.parse(dateFrom, dateTimeFormatter),
    dateTo = LocalDate.parse(dateTo, dateTimeFormatter),
    articleAction = artikkelHandling,
    articleType = articleType,
    functionalChange = funksjonsendring,
    forChildren = forChildren,
    supplierName = supplierName,
    supplierCity = supplierCity,
    mainProduct = mainProduct,
    sparePart = sparePart,
    accessory = accessory,
)


val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

