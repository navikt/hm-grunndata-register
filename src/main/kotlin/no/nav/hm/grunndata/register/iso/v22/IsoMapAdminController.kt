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
import java.util.UUID

@Secured(Roles.ROLE_ADMIN)
@Controller(IsoMapAdminController.API_V1_ADMIN_ISOMAP)
class IsoMapAdminController(private val isoMapRepository: IsoMapRepository) {
    companion object {
        const val API_V1_ADMIN_ISOMAP = "/admin/api/v1/isomap"
    }

    @Get("/")
    suspend fun getAllIsoMaps(): List<IsoMapDTO> {
        return isoMapRepository.findAll().toList().map { it.toDTO() }
    }

    @Post("/")
    suspend fun createIsoMap(isoMap: IsoMapDTO): HttpResponse<IsoMapDTO> = isoMapRepository.findByCode16AndCode22(isoMap.code16!!, isoMap.code22!!)?.let {
            throw BadRequestException("IsoMap ${isoMap.code16} -> ${isoMap.code22} already exists")
        } ?: HttpResponse.created(isoMapRepository.save(isoMap.toEntity()).toDTO())


    @Put("/{id}")
    suspend fun updateIsoMap(id: UUID, isoMap: IsoMapDTO): HttpResponse<IsoMapDTO> = isoMapRepository.findById(id)?.let { inDb ->
            HttpResponse.ok(isoMapRepository.update(isoMap.copy(id = inDb.id, created=inDb.created, ).toEntity()).toDTO())
        } ?: HttpResponse.notFound()

    suspend fun getVerifiedPercentage(): Int {
        val total = isoMapRepository.count()
        val verified = isoMapRepository.countVerified()
        return if (total == 0L) 0 else (verified * 100 / total).toInt()
    }

    fun IsoMap.toDTO(): IsoMapDTO = IsoMapDTO(
        id = this.id,
        code16 = this.code16,
        code22 = this.code22,
        mapEnum = this.mapEnum,
        created = this.created,
        verified = this.verified,
        level22 = this.level22
    )

    fun IsoMapDTO.toEntity(): IsoMap = IsoMap(
        id = this.id,
        code16 = this.code16,
        code22 = this.code22,
        mapEnum = this.mapEnum,
        created = this.created,
        verified = this.verified,
        level22 = this.level22
    )
}