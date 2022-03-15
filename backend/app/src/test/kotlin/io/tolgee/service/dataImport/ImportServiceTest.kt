package io.tolgee.service.dataImport

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.ImportTestData
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class ImportServiceTest : AbstractSpringTest() {
  lateinit var importTestData: ImportTestData

  @BeforeEach
  fun setup() {
    importTestData = ImportTestData()
    importTestData.addFrenchTranslations()
  }

  @Test
  fun `it selects existing language`() {
    importTestData.setAllResolved()
    testDataService.saveTestData(importTestData.root)
    importService.selectExistingLanguage(importTestData.importFrench, importTestData.french)
    assertThat(importTestData.importFrench.existingLanguage).isEqualTo(importTestData.french)
    val translations = importService.findTranslations(importTestData.importFrench.id)
    assertThat(translations[0].conflict).isNotNull
    assertThat(translations[1].conflict).isNull()
  }

  @Test
  fun `deletes import language`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)
    assertThat(importService.findLanguage(testData.importEnglish.id)).isNotNull
    importService.deleteLanguage(testData.importEnglish)
    entityManager.flush()
    entityManager.clear()
    assertThat(importService.findLanguage(testData.importEnglish.id)).isNull()
  }

  @Test
  fun `deletes import`() {
    val testData = ImportTestData()
    testData.addFileIssues()
    testData.addKeyMetadata()
    testDataService.saveTestData(testData.root)
    entityManager.flush()
    entityManager.clear()
    importService.deleteImport(testData.import)
    entityManager.flush()
    entityManager.clear()
    assertThat(importService.find(testData.import.project.id, testData.import.author.id)).isNull()
  }
}
