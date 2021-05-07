package io.tolgee.repository.dataImport

import io.tolgee.AbstractSpringTest
import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.development.testDataBuilder.data.ImportTestData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.testng.annotations.Test

@SpringBootTest
class ImportLanguageRepositoryTest : AbstractSpringTest() {

    @Autowired
    lateinit var importLanguageRepository: ImportLanguageRepository

    @Test
    fun `view query returns correct result`() {
        val testData = ImportTestData()
        testData.addFileIssues()
        testDataService.saveTestData(testData.root)
        val result = importLanguageRepository
                .findImportLanguagesView(testData.import.id, PageRequest.of(0, 10)).content

        assertThat(result).hasSize(3)
        assertThat(result[0].existingLanguageName).isEqualTo("English")
        assertThat(result[0].conflictCount).isEqualTo(4)
        assertThat(result[0].totalCount).isEqualTo(6)
        assertThat(result[0].resolvedCount).isEqualTo(0)
        assertThat(result[0].importFileIssueCount).isEqualTo(3)
    }

    @Test
    fun `deletes language`() {
        val testData = ImportTestData()
        testDataService.saveTestData(testData.root)
        entityManager.flush()
        entityManager.clear()
        importLanguageRepository.deleteById(testData.importEnglish.id)
        entityManager.flush()
        entityManager.clear()
        assertThat(importLanguageRepository.findById(testData.importEnglish.id)).isEmpty
    }
}
