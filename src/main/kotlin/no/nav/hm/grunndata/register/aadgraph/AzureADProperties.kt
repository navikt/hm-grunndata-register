package no.nav.hm.grunndata.register.aadgraph

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("azure.ad")
class AzureADProperties(
    var tenantId: String? = null,
    var clientId: String? = null,
    var clientSecret: String? = null,
    var userPrincipal: String? = null,
)
