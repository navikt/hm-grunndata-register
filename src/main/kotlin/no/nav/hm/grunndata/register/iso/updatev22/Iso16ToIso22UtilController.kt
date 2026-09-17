package no.nav.hm.grunndata.register.iso.updatev22

import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.iso.v22.Iso22
import no.nav.hm.grunndata.register.iso.v22.Iso22Repository
import no.nav.hm.grunndata.register.iso.v22.IsoMap
import no.nav.hm.grunndata.register.iso.v22.IsoMapEnum
import no.nav.hm.grunndata.register.iso.v22.IsoMapRepository
import no.nav.hm.grunndata.register.iso.v22.IsoType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller(Iso16ToIso22UtilController.INTERNAL_ISO22_UTIL)
@Tag(name="Admin IsoCategory")
    class Iso16ToIso22UtilController(private val iso16ToIso22Util: Iso16ToIso22Util,
                                     private val iso22Repository: Iso22Repository,
                                     private val isoMapRepository: IsoMapRepository
) {

    @Post("/upload")
    suspend fun uploadList(@Body isos: List<Iso22>) {
        LOG.info("Got iso22: $isos")
        // All standard ISO only have 3 levels and should never have '9' on the third level.
        isos.forEach { iso22 ->
            if (iso22.isoCode.length == 6 && iso22.isoCode[4] == '9') {
                throw BadRequestException("Iso22 ${iso22.isoCode} has '9' on the third level, which is not allowed for standard ISO categories")
            }
            if (iso22.isoType == IsoType.ISO && iso22.isoCode.length > 6) {
                throw BadRequestException("Iso22 ${iso22.isoCode} is a standard ISO category and should not have more than 3 levels")
            }
        }
        isos.forEach { iso22 ->
            iso22Repository.findByIsoCode(iso22.isoCode)?.let {
                iso22Repository.update(iso22.copy(id = it.id, created = it.created))
            } ?:run {
                iso22Repository.save(iso22)
            }
        }
    }

    @Post("/upload-mapping")
    suspend fun updateIsoMapping(@Body mappings: List<IsoMap>) {
        LOG.info("Got iso22 mappings: $mappings")
        // verify everything is ok before saving, if not throw exception
        mappings.forEach { mapping ->
            if (mapping.mapEnum.isEmpty()) {
                throw BadRequestException("Mapping for code16: ${mapping.code16} and code22: ${mapping.code22} is empty")
            }
        }
        mappings.filter { !it.code16.isNullOrEmpty() }.distinctBy { "${it.code16}-${it.code22}" }.forEach { mapping ->
            isoMapRepository.findById(mapping.id)?.let {
                isoMapRepository.update(mapping.copy(id = it.id, created = it.created, verified = mapping.mapEnum.contains(
                    IsoMapEnum.SAME)))
            } ?: run {
                isoMapRepository.save(mapping.copy(verified = mapping.mapEnum.contains(IsoMapEnum.SAME)) )
            }
        }
    }

    @Post("/rebuild-nat-mapping")
    suspend fun updateIsoNatMapping() {
        iso16ToIso22Util.rebuildIso16NatTo22Map()
    }

    @Get("/check-mapping")
    suspend fun checkIsoMapping() {
        iso16ToIso22Util.checkCode22Mappings()
    }

    @Post("/rebuild-iso22-tree")
    suspend fun rebuildIso22Tree() {
        iso16ToIso22Util.rebuildIso22TreeBasedOnVerifiedMapping()
    }

    companion object {
        const val INTERNAL_ISO22_UTIL = "/internal/iso22-util"
        private val LOG: Logger = LoggerFactory.getLogger(Iso16ToIso22UtilController::class.java)
    }
}