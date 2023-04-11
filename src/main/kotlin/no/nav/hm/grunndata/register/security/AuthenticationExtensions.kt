package no.nav.hm.grunndata.register.security

import io.micronaut.security.authentication.Authentication
import no.nav.hm.grunndata.register.user.UserAttribute
import java.util.*

fun Authentication.supplierId(): UUID = UUID.fromString(
    attributes[UserAttribute.SUPPLIER_ID] as String )
