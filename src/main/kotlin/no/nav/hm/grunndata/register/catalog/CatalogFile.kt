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
import no.nav.hm.grunndata.rapid.dto.CatalogFileRapidDTO
import no.nav.hm.grunndata.rapid.dto.CatalogFileStatus
import no.nav.hm.grunndata.rapid.dto.RapidDTO
import no.nav.hm.grunndata.register.event.EventPayload


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
    val connected: Boolean = false,
)

@Introspected
data class CatalogFileDTO(
    override val id: UUID,
    val fileName: String,
    val fileSize: Long,
    val orderRef: String,
    val supplierId: UUID,
    override val updatedByUser: String,
    val created: LocalDateTime,
    val updated: LocalDateTime,
    val status: CatalogFileStatus,
    val connected: Boolean = false,
): EventPayload {
    override fun toRapidDTO(): RapidDTO = CatalogFileRapidDTO(
        id = id,
        partitionKey = orderRef,
        fileName = fileName,
        fileSize = fileSize,
        orderRef = orderRef,
        supplierId = supplierId,
        updatedByUser = updatedByUser,
        created = created,
        updated = updated,
        status = status,
    )
}


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


fun CatalogImportExcelDTO.toEntity():CatalogImport  {
    val remapped = mapArticleType(articleType,funksjonsendring) // ensure articletype
    return CatalogImport(
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
        mainProduct = remapped.mainProduct,
        sparePart = remapped.sparePart,
        accessory = remapped.accessory,
    )
}


val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

