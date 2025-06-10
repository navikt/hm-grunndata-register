package no.nav.hm.grunndata.register.part

open class DuplicateLevartNummerException : RuntimeException {
    constructor(message: String?) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause)
}