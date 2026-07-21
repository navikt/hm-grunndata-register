package no.nav.hm.grunndata.register.iso.v22

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking

import org.junit.jupiter.api.Test

@MicronautTest
class IsoMapRepositoryTest(private val repo: IsoMapRepository) {

    @Test
    fun testCrudRepository() {
        val isoMap = IsoMap(
            code16 = "30300001",
            mapEnum = IsoMapEnum.SAME_OR_CHANGED_CODE_SAME_HEADER_MERGED,
            code22 = "20200001"
        )
        runBlocking {
            val saved = repo.save(isoMap)
            saved.shouldNotBeNull()
            val read = repo.findById(isoMap.id)
            read.shouldNotBeNull()
            read.code16 shouldBe "30300001"
            read.code22 shouldBe "20200001"
            read.mapEnum shouldBe IsoMapEnum.SAME_OR_CHANGED_CODE_SAME_HEADER_MERGED
            read.created.shouldNotBeNull()
        }
    }
}