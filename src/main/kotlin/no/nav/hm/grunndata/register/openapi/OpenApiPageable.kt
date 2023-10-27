package no.nav.hm.grunndata.register.openapi

import io.swagger.v3.oas.annotations.media.Schema

// For openapi spec
data class OpenApiPageable(
    @field:Schema(description = "page number", name = "number", example = "0", required = true)
    val number: Int,
    @field:Schema(description = "Page size", name="size", example = "100", required = true)
    val size: Int,
    @field:Schema(description = "Sort", name="sort", example = "updated,desc", required = true)
    val sort: String)