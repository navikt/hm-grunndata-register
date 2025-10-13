package no.nav.hm.grunndata.register.part.hidden

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.register.product.ProductDTOMapper
import no.nav.hm.grunndata.register.product.ProductRegistrationDTOV2
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory
import java.util.UUID

@Secured(Roles.ROLE_ADMIN)
@Controller(HiddenPartAdminController.API_V1_HIDDEN_PARTS)
@Tag(name = "Admin Hidden Parts")
class HiddenPartAdminController(
    private val hiddenPartService: HiddenPartService,
    private val productRegistrationService: ProductRegistrationService,
    private val productDTOMapper: ProductDTOMapper,
) {

    companion object {
        const val API_V1_HIDDEN_PARTS = "/admin/api/v1/part"
        private val LOG = LoggerFactory.getLogger(HiddenPartAdminController::class.java)
    }

    data class HideRequest(val reason: String? = null)

    data class HiddenPartDTO(
        val productId: UUID,
        val reason: String?,
        val created: String,
        val createdBy: String,
        val product: ProductRegistrationDTOV2? = null,
    )

    @Post("/{id}/hide")
    suspend fun hide(
        @PathVariable id: UUID,
        @Body body: HideRequest?,
        authentication: Authentication,
    ): HttpResponse<HiddenPartDTO> {
        val product = productRegistrationService.findById(id) ?: return HttpResponse.notFound()
        if (product.mainProduct) return HttpResponse.badRequest()
        val hidden = hiddenPartService.hide(id, body?.reason, authentication.name)
        LOG.info("Hidden part {} reason={} by {}", id, body?.reason, authentication.name)
        val dto = hidden.toDTO(productDTOMapper.toDTOV2(product))
        return HttpResponse.ok(dto)
    }

    @Delete("/{id}/hide")
    suspend fun unhide(
        @PathVariable id: UUID,
    ): HttpResponse<Unit> {
        hiddenPartService.unhide(id)
        LOG.info("Unhidden part {}", id)
        return HttpResponse.noContent()
    }

    @Get("/hidden")
    suspend fun listHidden(): List<HiddenPartDTO> {
        val hidden = hiddenPartService.listHidden()
        val result = mutableListOf<HiddenPartDTO>()
        for (hp in hidden) {
            val prod = productRegistrationService.findById(hp.productId)
            val dtoProd = prod?.let { productDTOMapper.toDTOV2(it) }
            result.add(hp.toDTO(dtoProd))
        }
        return result
    }

    private fun HiddenPart.toDTO(product: ProductRegistrationDTOV2?): HiddenPartDTO =
        HiddenPartDTO(
            productId = productId,
            reason = reason,
            created = created.toString(),
            createdBy = createdBy,
            product = product,
        )
}
