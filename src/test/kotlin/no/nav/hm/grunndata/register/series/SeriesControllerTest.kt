package no.nav.hm.grunndata.register.series

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.data.model.Page
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import jakarta.inject.Inject
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.CONTEXT_PATH
import no.nav.hm.grunndata.register.security.LoginClient
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationDTO
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserAttribute
import no.nav.hm.grunndata.register.user.UserControllerTest
import no.nav.hm.grunndata.register.user.UserRepository
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

@MicronautTest
class SeriesControllerTest(
    private val seriesRegistrationService: SeriesRegistrationService,
    private val loginClient: LoginClient,
    private val userRepository: UserRepository,
    private val supplierRegistrationService: SupplierRegistrationService,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(UserControllerTest::class.java)
    }

    @Inject
    @field:Client("$CONTEXT_PATH/")
    lateinit var client: HttpClient

    val email = "user33@test.test"
    val token = "token-123"

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @BeforeEach
    fun createUserSupplier() {
        runBlocking {
            val testSupplierRegistration = supplierRegistrationService.save(
                SupplierRegistrationDTO(
                    id = UUID.randomUUID(),
                    supplierData = SupplierData(
                        email = "supplier@test.test",
                        address = "address 1",
                        homepage = "https://www.hompage.no",
                        phone = "+47 12345678"
                    ),
                    identifier = "supplier-name",
                    name = "Supplier AB"
                )
            )
            val user = User(
                email = email, token = token,
                name = "User tester", roles = listOf(Roles.ROLE_SUPPLIER),
                attributes = mapOf(Pair(UserAttribute.SUPPLIER_ID, testSupplierRegistration.id.toString()))
            )
            userRepository.createUser(user)

            val serie = seriesRegistrationService.save(
                SeriesRegistrationDTO(
                    id = UUID.randomUUID(),
                    supplierId = testSupplierRegistration.id,
                    identifier = "testerino",
                    title = "superserie",
                    text = "text",
                    isoCategory = "12345678"
                )
            )

            val serie2 = seriesRegistrationService.save(
                SeriesRegistrationDTO(
                    id = UUID.randomUUID(),
                    supplierId = testSupplierRegistration.id,
                    identifier = "testtest",
                    title = "enda en serie",
                    text = "tekst",
                    isoCategory = "12345678"
                )
            )

            LOG.info("created supplier: ${objectMapper.writeValueAsString(testSupplierRegistration)}")
            LOG.info("created user: ${objectMapper.writeValueAsString(user)}")
            LOG.info("created series: ${objectMapper.writeValueAsString(serie)}")
            LOG.info("created series: ${objectMapper.writeValueAsString(serie2)}")
        }
    }


    @Test
    fun seriesApiTest() {
        val jwt = loginClient.login(UsernamePasswordCredentials(email, token)).getCookie("JWT").get()

        val allSeries = client.toBlocking().exchange(
            HttpRequest.GET<Page<SeriesRegistrationDTO>>(SeriesController.API_V1_SERIES)
                .accept(MediaType.APPLICATION_JSON)
                .cookie(jwt),
            Page::class.java
        )
            .shouldNotBeNull()
            .body().shouldNotBeNull()
        allSeries.totalSize shouldBe 2
        
        val uri = "${SeriesController.API_V1_SERIES}/?title=superserie"
        val filteredSeries = client.toBlocking().exchange(
            HttpRequest.GET<Page<SeriesRegistrationDTO>>(uri)
                .accept(MediaType.APPLICATION_JSON)
                .cookie(jwt),
            Page::class.java
        )
            .shouldNotBeNull()
            .body().shouldNotBeNull()
        filteredSeries.totalSize shouldBe 1
    }

}