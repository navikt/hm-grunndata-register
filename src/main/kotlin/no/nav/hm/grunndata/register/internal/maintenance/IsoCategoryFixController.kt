package no.nav.hm.grunndata.register.internal.maintenance

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import java.io.InputStreamReader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.hm.grunndata.register.iso.GdbApiClient
import no.nav.hm.grunndata.register.iso.IsoCategoryRegistrationDTO
import no.nav.hm.grunndata.register.iso.IsoCategoryRegistrationService
import no.nav.hm.grunndata.register.iso.IsoTranslations
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.reader.StreamReader

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/fix/category")
@Hidden
class IsoCategoryFixController(private val isoCategoryRegistrationService: IsoCategoryRegistrationService,
                               private val gdbApiClient: GdbApiClient,
                               private val objectMapper: ObjectMapper ) {


    @Put("/add-missing-category")
    suspend fun addMissingCategory() {
        val csvStream = IsoCategoryFixController::class.java.getResourceAsStream("/missing_categories.csv")
        val cvsParser = CSVParserBuilder().withSeparator(';').build()
        val reader = withContext(Dispatchers.IO) {
            CSVReaderBuilder(InputStreamReader(csvStream)).withCSVParser(cvsParser)
        }.build()

        var line: Array<String>?
        while (reader.readNext().also { line = it } != null) {
            val isocode = line?.get(0)!!.trim()
            val isoTitle = line?.get(1)!!.trim()
            LOG.info("adding missing category: $isocode;$isoTitle")
            isoCategoryRegistrationService.save(createIsoCategory(isocode, isoTitle))
        }
        reader.close()
    }

    @Put("/add-missing-short-title")
    suspend fun addMissingShortTitle() {
        val isos = gdbApiClient.retrieveIsoCategories()
        isos.forEach { iso ->
            if (iso.isoTitleShort != null) {
                isoCategoryRegistrationService.findByCode(iso.isoCode)?.let { inDb ->
                    isoCategoryRegistrationService.update(inDb.copy(isoTitleShort = iso.isoTitleShort))
                }
            }
        }
    }

    @Put("/fix-missing-searchwords")
    suspend fun fixMissingSearchWords() {
        val isos = objectMapper.readValue(IsoCategoryFixController::class.java.getResourceAsStream("/isos.json"),
            object : TypeReference<List<IsoCategoryRegistrationDTO>>(){})
        isos.forEach {
            if (it.searchWords.isNotEmpty()) {
                isoCategoryRegistrationService.findByCode(it.isoCode)?.let { iso ->
                    isoCategoryRegistrationService.update(iso.copy(searchWords = it.searchWords))
                } ?: isoCategoryRegistrationService.save(it)
            }
        }
    }

    private fun createIsoCategory(isocode: String, isoTitle: String): IsoCategoryRegistrationDTO {
        return IsoCategoryRegistrationDTO(
            isoCode = isocode,
            isoTitle = isoTitle,
            isoTitleShort = isoTitle,
            isoText = isoTitle,
            isoTextShort = isoTitle,
            isoTranslations = IsoTranslations(),
            isoLevel = isocode.length/2,
            isActive = true,
            showTech = true,
            allowMulti = true,
            createdByUser = "system",
            updatedByUser = "system"
        )
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(IsoCategoryFixController::class.java)
    }
}