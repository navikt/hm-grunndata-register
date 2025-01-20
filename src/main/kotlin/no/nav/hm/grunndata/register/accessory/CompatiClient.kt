package no.nav.hm.grunndata.register.accessory

import com.fasterxml.jackson.databind.JsonNode
import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client


@Client("\${grunndata.compati.url}")
interface CompatiClient  {

    @Get(uri="/catalog/products/compatibleWith", consumes = [APPLICATION_JSON])
    suspend fun findCompatibleWidth(@QueryValue("hmsNr") hmsNr: String,
                             @QueryValue("variant") boolean: Boolean? = false): JsonNode


}