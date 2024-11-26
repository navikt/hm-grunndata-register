package no.nav.hm.grunndata.register.aadgraph

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("azure.app")
class AzureADProperties(
    var tenantId: String = "dummy",
    var clientId: String = "dummy",
    var clientSecret: String = "dummy",
)
