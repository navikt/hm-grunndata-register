package no.nav.hm.grunndata.register.product.attributes

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.register.product.attributes.bestillingsordning.BestillingsordningService
import no.nav.hm.grunndata.register.product.attributes.digitalsoknadsortiment.DigitalSoknadSortimentService
import no.nav.hm.grunndata.register.product.attributes.paakrevdgodkjenningskurs.PaakrevdGodkjenningskursService
import no.nav.hm.grunndata.register.product.attributes.produkttype.ProdukttypeService
import org.slf4j.LoggerFactory

// TODO: REMOVE AGAIN AFTER TEST
@Controller(TempDebugController.API_V1_TEMP_TEST_BASEURL)
@Tag(name="temp test")
class TempDebugController(
    private val bestillingsordningService: BestillingsordningService,
    private val digitalSoknadSortimentService: DigitalSoknadSortimentService,
    private val paakrevdGodkjenningskursService: PaakrevdGodkjenningskursService,
    private val produkttypeService: ProdukttypeService,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(TempDebugController::class.java)
        const val API_V1_TEMP_TEST_BASEURL = "/temp-test"
    }

    @Post("/bestillingsordning")
    suspend fun bestillingsordning(): HttpResponse<String> {
        bestillingsordningService.importAndUpdateDb()
        return HttpResponse.ok("done.")
    }

    @Post("/digitalSoknadSortiment")
    suspend fun digitalSoknadSortiment(): HttpResponse<String> {
        digitalSoknadSortimentService.importAndUpdateDb()
        return HttpResponse.ok("done.")
    }

    @Post("/paakrevdGodkjenningskurs")
    suspend fun paakrevdGodkjenningskurs(): HttpResponse<String> {
        paakrevdGodkjenningskursService.importAndUpdateDb()
        return HttpResponse.ok("done.")
    }

    @Post("/produkttype")
    suspend fun produkttype(): HttpResponse<String> {
        produkttypeService.importAndUpdateDb()
        return HttpResponse.ok("done.")
    }
}
