package no.nav.hm.grunndata.register.internal.maintenance

import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.rapid.dto.*
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationRepository
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import no.nav.hm.grunndata.register.product.toMediaInfo
import no.nav.hm.grunndata.register.product.toProductData
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistration
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationRepository
import no.nav.hm.grunndata.register.series.SeriesDataDTO
import no.nav.hm.grunndata.register.series.SeriesRegistration
import no.nav.hm.grunndata.register.series.SeriesRegistrationRepository


@Hidden
@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/reimport-products-from-gdb")
open class ReActivateProductsFromGDbController(
    private val productAgreementRegistrationRepository: ProductAgreementRegistrationRepository,
    private val agreementRegistrationRepository: AgreementRegistrationRepository,
    private val productRegistrationRepository: ProductRegistrationRepository,
    private val seriesRegistrationRepository: SeriesRegistrationRepository,

    ) {

    @Post("/")
    @Transactional
    open suspend fun reActivateProductsFromGDb(@Body products: List<ProductRapidDTO>) {
        LOG.info("Got request to reimport ${products.size} products from GDB")
        products.forEach { dto ->
            productRegistrationRepository.findById(dto.id)?.let {
                LOG.info("Product ${dto.id} found in database")
            } ?: run {
                productRegistrationRepository.save(
                    ProductRegistration(
                        id = dto.id,
                        isoCategory = dto.isoCategory,
                        supplierId = dto.supplier.id,
                        supplierRef = dto.supplierRef,
                        seriesUUID = dto.seriesUUID ?: dto.id,
                        registrationStatus = mapStatus(dto.status),
                        adminStatus = mapAdminStatus(dto.status),
                        createdBy = dto.createdBy,
                        updatedBy = dto.updatedBy,
                        created = dto.created,
                        updated = dto.updated,
                        draftStatus = DraftStatus.DONE,
                        expired = dto.expired,
                        hmsArtNr = dto.hmsArtNr,
                        published = dto.published,
                        title = dto.title,
                        articleName = dto.articleName,
                        sparePart = dto.sparePart,
                        accessory = dto.accessory,
                        productData = dto.toProductData()
                    )
                )
                dto.seriesUUID?.let { uuid ->
                    seriesRegistrationRepository.findById(uuid) ?: seriesRegistrationRepository.save(
                        SeriesRegistration(
                            id = uuid,
                            supplierId = dto.supplier.id,
                            identifier = dto.seriesIdentifier ?: uuid.toString(),
                            title = dto.title,
                            text = dto.attributes.text ?: "",
                            isoCategory = dto.isoCategory,
                            draftStatus = DraftStatus.DONE,
                            status = SeriesStatus.ACTIVE,
                            adminStatus = AdminStatus.APPROVED,
                            createdBy = dto.createdBy,
                            updatedBy = dto.updatedBy,
                            created = dto.created,
                            updated = dto.updated,
                            published = dto.published,
                            expired = dto.expired,
                            seriesData = SeriesDataDTO(media = dto.media.map { it.toMediaInfo() }.toSet())
                        )
                    )
                }
            }
            dto.agreements.forEach { agreementInfo ->
                LOG.info("updating product agreement for product ${dto.id} with agreement_id ${agreementInfo.id} and postId ${agreementInfo.postId}")
                agreementRegistrationRepository.findById(agreementInfo.id)?.let { agreement ->
                    LOG.info("Agreement ${agreementInfo.id} found in database")
                    productAgreementRegistrationRepository.findByProductIdAndAgreementIdAndPostId(
                        dto.id, agreementInfo.id, agreementInfo.postId!!
                    )?.let { inDb ->
                        LOG.info("Product agreement ${agreementInfo.id} found in database")
                        productAgreementRegistrationRepository.update(
                            inDb.copy(
                                supplierId = dto.supplier.id,
                                supplierRef = dto.supplierRef,
                                productId = dto.id,
                                seriesUuid = dto.seriesUUID,
                                title = dto.title,
                                articleName = dto.articleName,
                                agreementId = agreementInfo.id,
                                hmsArtNr = dto.hmsArtNr,
                                post = agreementInfo.postNr,
                                rank = agreementInfo.rank,
                                postId = agreementInfo.postId!!,
                                reference = agreementInfo.reference,
                                expired = agreementInfo.expired,
                                created = dto.created,
                                updated = dto.updated,
                                createdBy = dto.createdBy,
                                sparePart = agreementInfo.sparePart,
                                accessory = agreementInfo.accessory,
                                published = agreementInfo.published!!,
                                status = agreementInfo.status,
                            )
                        )
                    } ?: run {
                        LOG.info("Product agreement ${agreementInfo.id} with postId ${agreementInfo.postId} not found in database, saving new")
                        productAgreementRegistrationRepository.save(
                            ProductAgreementRegistration(
                                supplierId = dto.supplier.id,
                                supplierRef = dto.supplierRef,
                                productId = dto.id,
                                seriesUuid = dto.seriesUUID,
                                title = dto.title,
                                articleName = dto.articleName,
                                agreementId = agreementInfo.id,
                                hmsArtNr = dto.hmsArtNr,
                                post = agreementInfo.postNr,
                                rank = agreementInfo.rank,
                                postId = agreementInfo.postId!!,
                                reference = agreementInfo.reference,
                                expired = agreementInfo.expired,
                                created = dto.created,
                                updated = dto.updated,
                                createdBy = dto.createdBy,
                                updatedBy = dto.updatedBy,
                                published = agreement.published,
                                status = agreementInfo.status,
                                sparePart = dto.sparePart,
                                accessory = dto.accessory
                            )
                        )
                    }
                }
            }

        }
    }

    private fun mapAdminStatus(status: ProductStatus): AdminStatus =
        if (status == ProductStatus.ACTIVE) AdminStatus.APPROVED else AdminStatus.PENDING

    private fun mapStatus(status: ProductStatus): RegistrationStatus =
        when (status) {
            ProductStatus.ACTIVE -> RegistrationStatus.ACTIVE
            ProductStatus.DELETED -> RegistrationStatus.DELETED
            else -> RegistrationStatus.INACTIVE
        }

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(ReActivateProductsFromGDbController::class.java)
    }
}
