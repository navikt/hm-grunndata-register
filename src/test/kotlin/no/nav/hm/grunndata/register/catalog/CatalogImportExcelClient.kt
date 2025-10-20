package no.nav.hm.grunndata.register.catalog

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.CookieValue
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.multipart.MultipartBody
import no.nav.hm.grunndata.register.CONTEXT_PATH
import no.nav.hm.grunndata.register.productagreement.CatalogImportResultReport
import no.nav.hm.grunndata.register.productagreement.ProductAgreementAdminController
import java.util.UUID

@Client("${CONTEXT_PATH}/${ProductAgreementAdminController.Companion.ADMIN_API_V1_PRODUCT_AGREEMENT}")
interface CatalogImportExcelClient {

    @Post("/excel-import", produces = ["multipart/form-data"], consumes = ["application/json"])
    fun excelImport(
        @CookieValue("JWT") jwt: String,
        @Body file: MultipartBody,
        @QueryValue dryRun: Boolean,
        @QueryValue supplierId: UUID
    ): HttpResponse<CatalogImportResultReport>

}