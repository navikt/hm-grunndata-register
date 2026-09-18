package no.nav.hm.grunndata.register.iso.updatev22

import jakarta.inject.Singleton
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import no.nav.hm.grunndata.register.iso.IsoCategoryRegistration
import no.nav.hm.grunndata.register.iso.IsoCategoryRegistrationRepository
import no.nav.hm.grunndata.register.iso.v22.Iso22
import no.nav.hm.grunndata.register.iso.v22.Iso22Repository
import no.nav.hm.grunndata.register.iso.v22.IsoMap
import no.nav.hm.grunndata.register.iso.v22.IsoMapEnum
import no.nav.hm.grunndata.register.iso.v22.IsoMapRepository
import no.nav.hm.grunndata.register.iso.v22.IsoMapper
import no.nav.hm.grunndata.register.iso.v22.IsoType
import no.nav.hm.grunndata.register.iso.v22.getLevelFromIsoCode
import no.nav.hm.grunndata.register.iso.v22.isOebsCategory
import org.slf4j.LoggerFactory

@Singleton
class Iso16ToIso22Util(private val isoCategoryRepository: IsoCategoryRegistrationRepository,
                       private val isoMapRepository: IsoMapRepository,
                       private val iso22Repository: Iso22Repository,
                       private val isoMapper: IsoMapper
) {


    suspend fun rebuildIso16NatTo22Map() {
        val iso16Nats = isoCategoryRepository.findAll().filter { it.isoLevel == 4 || isOebsCategory(it.isoCode) }.toList()
        val allIso16Maps = isoMapRepository.findAll().filter { !it.code16.isNullOrEmpty() }.map { it.code16 }.toSet()
        iso16Nats.forEach { iso16Nat ->
            if (!allIso16Maps.contains(iso16Nat.isoCode)) {
                LOG.info("Trying to find mapping for iso16: ${iso16Nat.isoCode}")
                createOrUpdateIso16To22Map(iso16Nat)
            }
        }

    }

    suspend fun createOrUpdateIso16To22Map(iso16Nat: IsoCategoryRegistration): IsoMap? {
        return isoMapper.mapIso16To22(iso16Nat.isoCode)?.let { isoMap ->
            LOG.info("Found mapping for iso16: ${iso16Nat.isoCode} to iso22: ${isoMap.code22}")
            if (isoMap.mapEnum.contains(IsoMapEnum.SAME)) {
                isoMapRepository.findByCode16AndCode22(iso16Nat.isoCode, iso16Nat.isoCode) ?: run {
                    isoMapRepository.save(
                        IsoMap(
                            code16 = iso16Nat.isoCode,
                            code22 = iso16Nat.isoCode,
                            mapEnum = listOf(IsoMapEnum.SAME),
                            verified = true
                        )
                    )
                }
            } else {
                if (isOebsCategory(iso16Nat.isoCode) && iso16Nat.isoCode.startsWith(isoMap.code22!!)) {
                    LOG.info("Found mapping for iso16: ${iso16Nat.isoCode} to iso22: ${isoMap.code22}, but not SAME code, but iso16 is oebs category and starts with iso22, so we can map it")
                    isoMapRepository.findByCode16AndCode22(iso16Nat.isoCode, iso16Nat.isoCode) ?: run {
                        isoMapRepository.save(
                            IsoMap(
                                code16 = iso16Nat.isoCode,
                                code22 = iso16Nat.isoCode,
                                mapEnum = isoMap.mapEnum,
                                verified = false
                            )
                        )
                    }
                } else {
                    LOG.info("Found mapping for iso16: ${iso16Nat.isoCode} to iso22: ${isoMap.code22}, but not SAME code, and iso22 is not level 3 or iso16 does not start with iso22")
                    isoMapRepository.findByCode16AndCode22(iso16Nat.isoCode, isoMap.code22!!) ?: run {
                        isoMapRepository.save(
                            IsoMap(
                                code16 = iso16Nat.isoCode,
                                code22 = isoMap.code22,
                                mapEnum = isoMap.mapEnum,
                                verified = false
                            )
                        )
                    }
                }
            }
        } ?: run {
            LOG.warn("Could not find mapping for iso16: ${iso16Nat.isoCode}")
            isoMapRepository.findByCode16AndCode22(iso16Nat.isoCode, "") ?: run {
                isoMapRepository.save(
                    IsoMap(
                        code16 = iso16Nat.isoCode,
                        code22 = "",
                        mapEnum = listOf(IsoMapEnum.DELETED_CLASS_OR_SUBCLASS_OR_SECTION),
                        verified = false
                    )
                )
            }
        }
    }

    suspend fun checkCode22Mappings() {
        val code22List = isoMapRepository.findAll().map { it.code22 }.toSet()
        code22List.forEach {
            code22 -> iso22Repository.findByIsoCode(code22!!)?.let {
            } ?: run {
                LOG.warn("Could not find iso22 for code22: $code22")
            }
        }
    }

    suspend fun rebuildIso22TreeBasedOnVerifiedMapping() {
        // rebuild iso22 tree based on iso16 to iso22 mapping, only the ones that are verified.
        val verifiedIsoMaps = isoMapRepository.findAll().filter { it.verified && (getLevelFromIsoCode(it.code22!!) == 4 || isOebsCategory(it.code22)) }.toList()
        verifiedIsoMaps.forEach { isoMap ->
            iso22Repository.findByIsoCode(isoMap.code22!!) ?: run {
                // level 4 iso22 does not exist, create it based on iso16
                isoCategoryRepository.findById(isoMap.code22)?.let { iso16 ->
                    LOG.info("Saving iso22 for code22: ${isoMap.code22} based on iso16: ${iso16.isoCode}")
                    iso22Repository.save(
                        Iso22(
                            isoCode = isoMap.code22,
                            isoTitle = iso16.isoTitle,
                            isoText = iso16.isoText,
                            createdByUser = "system",
                            updatedByUser = "system",
                            isoTranslations = iso16.isoTranslations,
                            isoType = if (isOebsCategory(isoMap.code22)) IsoType.OEBS else IsoType.NAT,
                            searchWords = iso16.searchWords
                        )
                    )
                }
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(Iso16ToIso22Util::class.java)
    }
}