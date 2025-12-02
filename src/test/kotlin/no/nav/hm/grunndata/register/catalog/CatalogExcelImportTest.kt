package no.nav.hm.grunndata.register.catalog

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AgreementDTO
import no.nav.hm.grunndata.rapid.dto.AgreementPost
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.agreement.AgreementData
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationDTO
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationService
import no.nav.hm.grunndata.register.agreement.DelkontraktData
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistration
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistrationRepository
import no.nav.hm.grunndata.register.security.LoginClient
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationDTO
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserRepository
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

@MicronautTest
class CatalogExcelImportTest(
    private val agreementRegistrationService: AgreementRegistrationService,
    private val supplierRegistrationService: SupplierRegistrationService,
    private val delkontraktRegistrationRepository: DelkontraktRegistrationRepository,
    private val userRepository: UserRepository,
    private val client: CatalogImportExcelClient,
    private val loginClient: LoginClient,
    private val catalogFileToProductAgreementScheduler: CatalogFileToProductAgreementScheduler


    ) {

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    val email = "ProductAgreementExcelImportTest@test.test"
    val password = "admin-123"
    val supplierId = UUID.randomUUID()

    companion object {
        private val LOG = LoggerFactory.getLogger(CatalogExcelImportTest::class.java)
    }

    init {
        runBlocking {

            val testSupplier = supplierRegistrationService.save(
                SupplierRegistrationDTO(
                    id = supplierId,
                    supplierData = SupplierData(
                        address = "address 4",
                        homepage = "https://www.hompage.no",
                        phone = "+47 12345678",
                        email = "supplier4@test.test",
                    ),
                    identifier = "$supplierId-unique-name",
                    name = "Leverandør AS -$supplierId"
                )
            ).toRapidDTO()
            userRepository.createUser(
                User(
                    email = email, token = password, name = "UserAdmin tester", roles = listOf(Roles.ROLE_ADMIN)
                )
            )
            val agreementId = UUID.randomUUID()
            val agreement = AgreementDTO(
                id = agreementId, published = LocalDateTime.now(),
                expired = LocalDateTime.now().plusYears(2), title = "Title of agreement",
                text = "some text", reference = "22-7601", identifier = "unik-ref1234", resume = "resume",
                posts = listOf(
                    AgreementPost(
                        identifier = "unik-post1", title = "Post title",
                        description = "post description", nr = 1
                    ), AgreementPost(
                        identifier = "unik-post2", title = "Post title 2",
                        description = "post description 2", nr = 2
                    )
                ), createdBy = REGISTER, updatedBy = REGISTER,
                created = LocalDateTime.now(), updated = LocalDateTime.now()
            )
            val delkontrakt1 = DelkontraktRegistration(
                id = UUID.randomUUID(),
                agreementId = agreementId,
                delkontraktData = DelkontraktData(
                    title = "1: Delkontrakt",
                    description = "Delkontrakt 1 description",
                    sortNr = 1,
                    refNr = "1"
                ),
                createdBy = REGISTER,
                updatedBy = REGISTER
            )

            val delkontrakt2 = DelkontraktRegistration(
                id = UUID.randomUUID(),
                agreementId = agreementId,
                delkontraktData = DelkontraktData(
                    title = "2: Delkontrakt",
                    description = "Delkontrakt 2 description",
                    sortNr = 2,
                    refNr = "2"
                ),
                createdBy = REGISTER,
                updatedBy = REGISTER
            )

            val delkontrakt1A = DelkontraktRegistration(
                id = UUID.randomUUID(),
                agreementId = agreementId,
                delkontraktData = DelkontraktData(
                    title = "1A: Delkontrakt",
                    description = "Delkontrakt 1A description",
                    sortNr = 3,
                    refNr = "1A"
                ),
                createdBy = REGISTER,
                updatedBy = REGISTER
            )


            val delkontrakt1B = DelkontraktRegistration(
                id = UUID.randomUUID(),
                agreementId = agreementId,
                delkontraktData = DelkontraktData(
                    title = "1B: Delkontrakt ",
                    description = "Delkontrakt 1B description",
                    sortNr = 3,
                    refNr = "1B"
                ),
                createdBy = REGISTER,
                updatedBy = REGISTER
            )

            val data = AgreementData(
                text = "some text", resume = "resume",
                identifier = UUID.randomUUID().toString()
            )

            val agreementRegistration = AgreementRegistrationDTO(
                id = agreementId,
                published = agreement.published,
                expired = agreement.expired,
                title = agreement.title,
                reference = agreement.reference,
                updatedByUser = "username",
                createdByUser = "username",
                agreementData = data
            )

            agreementRegistrationService.save(agreementRegistration)
            delkontraktRegistrationRepository.save(delkontrakt1)
            delkontraktRegistrationRepository.save(delkontrakt2)
            delkontraktRegistrationRepository.save(delkontrakt1A)
            delkontraktRegistrationRepository.save(delkontrakt1B)

        }
    }

    @Test
    fun testImportExcel() {
        runBlocking {
            val resp = loginClient.login(UsernamePasswordCredentials(email, password))
            val jwt = resp.getCookie("JWT").get().value
            val bytes1 =
                CatalogExcelImportTest::class.java.getResourceAsStream("/productagreement/katalog-test.xls")
                    .readAllBytes()
            val multipartBody1 = MultipartBody
                .builder()
                .addPart(
                    "file", "katalog-test.xls",
                    MediaType.MICROSOFT_EXCEL_TYPE, bytes1
                )
                .build()
            val response = client.excelImport(jwt, multipartBody1, false, supplierId)
            response.status shouldBe HttpStatus.OK
            val body = response.body()
            body.shouldNotBeNull()
            val (productAgreementMappedResultLists, serviceAgreementMappedResultLists)= catalogFileToProductAgreementScheduler.processCatalogFile() ?: throw IllegalStateException("No catalog file to process")
            productAgreementMappedResultLists.insertList.size shouldBe 8
            serviceAgreementMappedResultLists.insertList.size shouldBe 0
            val bytes2 =
                CatalogExcelImportTest::class.java.getResourceAsStream("/productagreement/katalog-test-2.xls")
                    .readAllBytes()
            val multipartBody2 = MultipartBody
                .builder()
                .addPart(
                    "file", "katalog-test-2.xls",
                    MediaType.MICROSOFT_EXCEL_TYPE, bytes2
                )
                .build()
            val response2 = client.excelImport(jwt, multipartBody2, false, supplierId)
            response.status shouldBe HttpStatus.OK
            val body2 = response2.body()
            body2.shouldNotBeNull()
            val (pResult, sResult) = catalogFileToProductAgreementScheduler.processCatalogFile()?: throw IllegalStateException("No catalog file to process")
            pResult.insertList.size shouldBe 2
            pResult.deactivateList.size shouldBe 2
            sResult.updateList.size shouldBe 0

        }
    }
}