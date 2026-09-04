package no.nav.hm.grunndata.register.iso.v22

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.toList
import no.nav.hm.grunndata.register.error.BadRequestException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller(Iso22AdminController.API_V1_ADMIN_ISO22)
@Tag(name="Admin IsoCategory")
class Iso22AdminController(private val iso22Repository: Iso22Repository,
                           private val isoMapRepository: IsoMapRepository,
                           private val iso16ToIso22Util: Iso16ToIso22Util) {


    @Post("/upload")
    suspend fun uploadList(@Body isos: List<Iso22>) {
        LOG.info("Got iso22: $isos")
        isos.forEach { iso22 ->
            iso22Repository.findByIsoCode(iso22.isoCode)?.let {
                iso22Repository.update(iso22.copy(id = it.id, created = it.created))
            } ?:run {
                iso22Repository.save(iso22)
            }
        }
    }

    @Post("/mapping")
    suspend fun updateIsoMapping(@Body mappings: List<IsoMap>) {
        LOG.info("Got iso22 mappings: $mappings")
        // verify everything is ok before saving, if not throw exception
        mappings.forEach { mapping ->
            if (mapping.mapEnum.isEmpty()) {
                throw BadRequestException("Mapping for code16: ${mapping.code16} and code22: ${mapping.code22} is empty")
            }
        }
        mappings.forEach { mapping ->

            isoMapRepository.findById(mapping.id)?.let {
                isoMapRepository.update(mapping.copy(id = it.id, created = it.created, verified = mapping.mapEnum.contains(IsoMapEnum.SAME)))
            } ?: run {
                isoMapRepository.save(mapping)
            }
        }
    }

    @Post("/nat-mapping")
    suspend fun updateIsoNatMapping(@Body mappings: List<IsoMap>) {
        iso16ToIso22Util.rebuildIso16NatTo22Map()
    }

    @Get("/check-mapping")
    suspend fun checkIsoMapping() {
        iso16ToIso22Util.checkCode22Mappings()
    }

    @Post("/rebuild-iso22-tree")
    suspend fun rebuildIso22Tree() {
        iso16ToIso22Util.rebuildIso22Tree()
    }

    @Get("/")
    suspend fun getAllIsos(): List<Iso22> {
        return iso22Repository.findAll().toList()
    }


    @Post("/")
    suspend fun createIso(@Body iso: Iso22, authentication: Authentication): HttpResponse<Iso22> =
        iso22Repository.findByIsoCode(iso.isoCode)?.let {
            throw BadRequestException("Iso22 ${iso.isoCode} already exists")
        } ?: HttpResponse.created(iso22Repository.save(iso.copy(createdByUser = authentication.name,
            updatedByUser = authentication.name, created = LocalDateTime.now(), updated = LocalDateTime.now())))


    @Put("/{isocode}")
    suspend fun updateIsoByIsocode(isocode: String, @Body iso: Iso22, authentication: Authentication): HttpResponse<Iso22> =
        iso22Repository.findByIsoCode(isocode)?.let { inDb ->
            HttpResponse.ok(iso22Repository.update(iso.copy(id = inDb.id, created = inDb.created,
                createdByUser = inDb.createdByUser, updatedByUser = authentication.name, updated = LocalDateTime.now())))
        } ?: HttpResponse.notFound()

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(Iso22AdminController::class.java)
        const val API_V1_ADMIN_ISO22 = "/api/v1/admin/iso22"
    }


}