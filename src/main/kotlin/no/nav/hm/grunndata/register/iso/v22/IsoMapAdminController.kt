package no.nav.hm.grunndata.register.iso.v22

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import kotlinx.coroutines.flow.toList
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.security.Roles

@Secured(Roles.ROLE_ADMIN)
@Controller(IsoMapAdminController.API_V1_ADMIN_ISOMAP)
class IsoMapAdminController(private val isoMapRepository: IsoMapRepository) {
    companion object {
        const val API_V1_ADMIN_ISOMAP = "/admin/api/v1/isomap"
    }

    @Get("/")
    suspend fun getAllIsoMaps(): List<IsoMap> {
        return isoMapRepository.findAll().toList()
    }

    @Post("/")
    suspend fun createIsoMap(isoMap: IsoMap): HttpResponse<IsoMap> = isoMapRepository.findByCode16AndCode22(isoMap.code16, isoMap.code22)?.let {
            throw BadRequestException("IsoMap ${isoMap.code16} -> ${isoMap.code22} already exists")
        } ?: HttpResponse.created(isoMapRepository.save(isoMap))


    @Put("/")
    suspend fun updateIsoMap(isoMap: IsoMap): HttpResponse<IsoMap> = isoMapRepository.findById(isoMap.id)?.let { inDb ->
            HttpResponse.ok(isoMapRepository.update(isoMap.copy(id = inDb.id)))
        } ?: HttpResponse.notFound()

}