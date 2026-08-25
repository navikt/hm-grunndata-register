package no.nav.hm.grunndata.register.aadgraph

import com.azure.identity.ClientSecretCredential
import com.azure.identity.ClientSecretCredentialBuilder
import com.microsoft.graph.models.BodyType
import com.microsoft.graph.models.EmailAddress
import com.microsoft.graph.models.ItemBody
import com.microsoft.graph.models.Message
import com.microsoft.graph.models.Recipient
import com.microsoft.graph.serviceclient.GraphServiceClient
import com.microsoft.graph.users.item.sendmail.SendMailPostRequestBody
import jakarta.inject.Singleton
import java.util.LinkedList
import org.slf4j.LoggerFactory

@Singleton
open class EmailServiceAzure(private val aadProperties: AzureADProperties) : EmailService {
    val scopes = "https://graph.microsoft.com/.default"

    companion object {
        private val LOG = LoggerFactory.getLogger(EmailServiceAzure::class.java)
        private val SECURE_LOG = LoggerFactory.getLogger(EmailServiceAzure::class.java.name + ".secure")
    }

    val credential: ClientSecretCredential? =
        ClientSecretCredentialBuilder()
            .clientId(aadProperties.clientId)
            .tenantId(aadProperties.tenantId)
            .clientSecret(aadProperties.clientSecret)
            .build()

    val graphClient: GraphServiceClient = GraphServiceClient(credential, scopes)

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

        val sendMailPostRequestBody = SendMailPostRequestBody()
        sendMailPostRequestBody.message = message;
        sendMailPostRequestBody.saveToSentItems = true;

        kotlin.runCatching {
            graphClient.users().byUserId("ikke.svar.finnhjelpemiddel@nav.no").sendMail().post(sendMailPostRequestBody)
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
