package no.nav.hm.grunndata.register.catalog

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import io.micronaut.http.multipart.CompletedFileUpload
import java.io.ByteArrayInputStream
import java.time.LocalDateTime
import java.util.UUID

@MappedEntity("catalog_file_v1")
data class CatalogFile(
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val size: Long,
    @field:TypeDef(type = DataType.BYTE_ARRAY)
    val file: ByteArray,
    val supplierId: UUID,
    val createdBy: String,
    val created: LocalDateTime,
    val updated: LocalDateTime,
    val status: CatalogFileStatus = CatalogFileStatus.PENDING,
)

@Introspected
data class CatalogFileDTO(
    val id: UUID,
    val name: String,
    val size: Long,
    val supplierId: UUID,
    val createdBy: String,
    val created: LocalDateTime,
    val updated: LocalDateTime,
    val status: CatalogFileStatus,
)

enum class CatalogFileStatus {
    PENDING, DONE, ERROR
}
