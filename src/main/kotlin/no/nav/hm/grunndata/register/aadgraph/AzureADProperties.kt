package no.nav.hm.grunndata.register.aadgraph

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("azure.app")
class AzureADProperties(
    var tenantId: String? = null,
    var clientId: String? = null,
    var clientSecret: String? = null,
)
