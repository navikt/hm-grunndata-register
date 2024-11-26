package no.nav.hm.grunndata.register.aadgraph

import com.azure.identity.ClientSecretCredentialBuilder
import com.microsoft.graph.authentication.TokenCredentialAuthProvider
import com.microsoft.graph.models.BodyType
import com.microsoft.graph.models.EmailAddress
import com.microsoft.graph.models.ItemBody
import com.microsoft.graph.models.Message
import com.microsoft.graph.models.Recipient
import com.microsoft.graph.models.UserSendMailParameterSet
import com.microsoft.graph.requests.GraphServiceClient
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.LinkedList

@Singleton
open class EmailServiceAzure(private val aadProperties: AzureADProperties) : EmailService {

    val scopes = listOf("https://graph.microsoft.com/.default")

    companion object {
        private val LOG = LoggerFactory.getLogger(EmailServiceAzure::class.java)
        private val SECURE_LOG = LoggerFactory.getLogger(EmailServiceAzure::class.java.name + ".secure")
    }

    val credential =
        ClientSecretCredentialBuilder()
            .clientId(aadProperties.clientId)
            .tenantId(aadProperties.tenantId)
            .clientSecret(aadProperties.clientSecret)
            .build()

    val authProvider = TokenCredentialAuthProvider(
        scopes,
        credential,
    )

    val graphClient: GraphServiceClient<okhttp3.Request> = GraphServiceClient.builder()
        .authenticationProvider(authProvider).buildClient()

    override fun sendSimpleMessage(
        to: String,
        subject: String,
        contentType: BodyType,
        content: String,
    ) {
        val message = Message()

        val toRecipientsList: LinkedList<Recipient> = LinkedList<Recipient>()
        val toRecipients = Recipient()
        val emailAddress = EmailAddress()
        emailAddress.address = to
        toRecipients.emailAddress = emailAddress
        toRecipientsList.add(toRecipients)
        message.toRecipients = toRecipientsList

        message.subject = subject

        val body = ItemBody()
        body.contentType = contentType
        body.content = content
        message.body = body

        kotlin.runCatching {
            graphClient.users(aadProperties.userPrincipal!!).sendMail(
                UserSendMailParameterSet
                    .newBuilder()
                    .withMessage(message)
                    .withSaveToSentItems(true)
                    .build(),
            )
                .buildRequest()
                .post()
        }
            .onSuccess {
                LOG.info("mail sent")
            }
            .onFailure {
                LOG.error("Got error", it)
                SECURE_LOG.error("Got error sending mail to ${message.toRecipients!![0].emailAddress!!.address}")
            }
    }
}
