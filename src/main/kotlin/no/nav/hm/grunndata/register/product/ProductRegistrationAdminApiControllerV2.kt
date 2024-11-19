package no.nav.hm.grunndata.register.product

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory

@Secured(Roles.ROLE_ADMIN)
@Controller(ProductRegistrationAdminApiControllerV2.API_V2_ADMIN_PRODUCT_REGISTRATIONS)
@Tag(name = "Admin Product V2")
class ProductRegistrationAdminApiControllerV2(
    private val productRegistrationService: ProductRegistrationService,
    private val productDTOMapper: ProductDTOMapper,
) {
    companion object {
        const val API_V2_ADMIN_PRODUCT_REGISTRATIONS = "/admin/api/v2/product/registrations"
        private val LOG = LoggerFactory.getLogger(ProductRegistrationAdminApiControllerV2::class.java)
    }


    @Get("/series/{seriesUUID}")
    suspend fun findBySeriesUUIDAndSupplierId(seriesUUID: UUID) =
        productRegistrationService.findAllBySeriesUuid(seriesUUID).sortedBy { it.created }
            .map { productDTOMapper.toDTOV2(it) }

}
