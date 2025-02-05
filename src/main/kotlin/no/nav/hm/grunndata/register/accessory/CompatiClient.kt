package no.nav.hm.grunndata.register.accessory

import com.fasterxml.jackson.databind.JsonNode
import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client


@Client("\${grunndata.compati.url}")
interface CompatiClient  {

    @Get(uri="/catalog/products/compatibleWith", consumes = [APPLICATION_JSON])
    suspend fun findCompatibleWith(@QueryValue("hmsNr") hmsNr: String,
                                    @QueryValue("variant") boolean: Boolean? = false): List<CompatibleProductResult>



}

data class CompatibleProductResult(
    val score: Double,
    val title: String,
    val seriesTitle: String,
    val seriesId: String,
    val productId: String,
    val hmsArtNr: String,
)