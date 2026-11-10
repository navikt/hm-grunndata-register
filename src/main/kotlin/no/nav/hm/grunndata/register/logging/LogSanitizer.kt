package no.nav.hm.grunndata.register.logging

private val logControlCharacters = Regex("[\\x00-\\x1F\\x7F-\\x9F\\u2028\\u2029]")
private const val MAX_LOG_VALUE_LENGTH = 200

fun String?.sanitizeForLog(): String {
    val sanitized = (this ?: "null").replace(logControlCharacters, "_")
    return if (sanitized.length > MAX_LOG_VALUE_LENGTH) {
        sanitized.take(MAX_LOG_VALUE_LENGTH) + "...(truncated)"
    } else {
        sanitized
    }
}
