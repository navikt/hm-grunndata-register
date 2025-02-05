package no.nav.hm.grunndata.register.accessory

import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.rapids_rivers.toUUID
import no.nav.hm.grunndata.register.product.ProductData
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import no.nav.hm.grunndata.register.security.LoginClient
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserRepository
import org.junit.jupiter.api.Test

@MicronautTest
class AccessoryCompatibleWithTest(
    private val userRepository: UserRepository,
    private val loginClient: LoginClient,
    private val productRegistrationRepository: ProductRegistrationRepository,
    private val accessoryCompatibleWithApiClient: AccessoryCompatibleWithApiClient
) {

    val userEmail = "accessory1@test.test"
    val password = "test123"
    val productId = UUID.randomUUID()

    @MockBean(CompatiClient::class)
    fun mockCompatibleFinder(): CompatiClient = mockk<CompatiClient>(relaxed = true).apply {
        coEvery {
            findCompatibleWith("229391", true)
        } coAnswers {
            listOf<CompatibleProductResult>(
                CompatibleProductResult(
                    hmsArtNr = "292222",
                    title = "Vendehjm personvender TurnAid T4sidegrinder man trekklaken/glidelaken  sengeb90  belastningsvekt350",
                    seriesTitle = "TurnAid T4",
                    seriesId = "35574933-cac9-4704-a702-a5e2f55f48eb",
                    productId = "35dfc205-223e-4723-9ed1-81de2beefb57",
                    score = 0.0,
                )

            )
        }
    }

    init {
        runBlocking {
            val hmsUser = userRepository.createUser(
                User(
                    email = userEmail, token = password, name = "HMS tester", roles = listOf(Roles.ROLE_HMS)
                )
            )
            val product = productRegistrationRepository.save(
                ProductRegistration(
                    id = productId,
                    title = "Test accessory product",
                    hmsArtNr = "229391",
                    seriesId = UUID.randomUUID().toString(),
                    seriesUUID = UUID.randomUUID(),
                    articleName = "Test article",
                    supplierId = UUID.randomUUID(),
                    supplierRef = "Supplier ref1",
                    productData = ProductData(),
                    accessory = true,
                    created = LocalDateTime.now()
                )
            )
        }
    }

    @Test
    fun testAccessoryCompatibleWith() {
        runBlocking {
            val jwtHms = loginClient.login(UsernamePasswordCredentials(userEmail, password))
                .getCookie("JWT").get().value

            val compatibleProductResults =
                accessoryCompatibleWithApiClient.findCompatibleWithProductsVariants(jwtHms, "229391")
            compatibleProductResults.shouldNotBeNull()
            compatibleProductResults.size shouldBe 1
            val item = compatibleProductResults[0]
            val compatibleWithDTO = CompatibleWithDTO(
                seriesIds = setOf(item.seriesId.toUUID()),
                productIds = setOf(item.productId.toUUID())
            )
            val connectedProduct = accessoryCompatibleWithApiClient.connectProductAndVariants(jwtHms, compatibleWithDTO, productId)
            connectedProduct.shouldNotBeNull()
            connectedProduct.articleName shouldBe "Test article"
            connectedProduct.productData.attributes.compatibleWith.shouldNotBeNull()
            connectedProduct.productData.attributes.compatibleWith!!.seriesIds.size shouldBe 1
            connectedProduct.productData.attributes.compatibleWith!!.productIds.size shouldBe 1
        }
    }
}