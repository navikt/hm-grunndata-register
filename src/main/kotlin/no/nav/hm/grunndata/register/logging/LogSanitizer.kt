package no.nav.hm.grunndata.register.logging

private val logControlCharacters = Regex("[\\x00-\\x1F\\x7F-\\x9F\\u2028\\u2029]")

fun String?.sanitizeForLog(): String = (this ?: "null").replace(logControlCharacters, "_")
