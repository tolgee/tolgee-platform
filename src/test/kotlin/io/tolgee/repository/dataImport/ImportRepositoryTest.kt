package io.tolgee.repository.dataImport

import io.tolgee.AbstractSpringTest
import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.development.testDataBuilder.data.ImportTestData
import io.tolgee.model.dataImport.Import
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.testng.annotations.Test

@SpringBootTest
class ImportRepositoryTest : AbstractSpringTest() {

    @Autowired
    lateinit var importRepository: ImportRepository

    @Test
    fun `creates, saves and gets Import entity`() {
        val base = dbPopulator.createBase("hello", "importUser")
        Import(author = base.userOwner!!, repository = base).let {
            importRepository.save(it).let {
                importRepository.getOne(it.id).let { got ->
                    assertThat(got.author).isEqualTo(base.userOwner)
                    assertThat(got.repository).isEqualTo(base)
                    assertThat(got.id).isGreaterThan(0L)
                }
            }
        }
    }

    @Test
    fun `deletes import`() {
        val testData = ImportTestData()
        testDataService.saveTestData(testData.root)
        entityManager.flush()
        entityManager.clear()
        importRepository.deleteById(testData.import.id)
        importRepository.flush()
        entityManager.clear()
        assertThat(importRepository.findById(testData.import.id)).isEmpty
    }
}
