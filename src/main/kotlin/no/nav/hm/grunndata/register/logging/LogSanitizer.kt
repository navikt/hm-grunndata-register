package no.nav.hm.grunndata.register.logging

fun String?.sanitizeForLog(): String = (this ?: "null").replace('\r', '_').replace('\n', '_')
