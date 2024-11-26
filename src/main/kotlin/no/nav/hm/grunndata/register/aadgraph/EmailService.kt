package no.nav.hm.grunndata.register.aadgraph

import com.microsoft.graph.models.BodyType

interface EmailService {
    fun sendSimpleMessage(
        to: String,
        subject: String,
        contentType: BodyType,
        content: String,
    )
}
