package no.nav.hm.grunndata.register.leaderelection
import io.micronaut.context.annotation.Value
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.annotation.Client
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.net.InetAddress

@Singleton
class LeaderElection(private val client: GetLeaderClient,
                     @Value("\${elector.path}") val electorPath: String) {

    private val hostname = InetAddress.getLocalHost().hostName

    init {
        LOG.info("leader election initialized this hostname is $hostname")
        LOG.info("Is Leader: ${isLeader()}")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(LeaderElection::class.java)
    }

    fun isLeader(): Boolean = if (electorPath =="localhost") true else hostname == getLeader()

    private fun getLeader(): String = client.getLeader().name

}


@Client("http://\${elector.path}")
interface GetLeaderClient {
    @Get("/")
    fun getLeader(): Elector

}

data class Elector(val name: String)
