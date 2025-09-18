package no.nav.hm.grunndata.register.product.workswith

import io.micronaut.core.annotation.Introspected
import java.util.UUID

@Introspected
data class WorksWithMapping(val sourceProductId: UUID, val targetProductId: UUID)
