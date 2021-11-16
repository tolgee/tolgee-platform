package io.tolgee.service.dataImport

import io.tolgee.AbstractSpringTest
import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.development.testDataBuilder.data.ImportTestData
import org.springframework.boot.test.context.SpringBootTest
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

@SpringBootTest
class ImportServiceTest : AbstractSpringTest() {
  lateinit var importTestData: ImportTestData

  @BeforeMethod
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
    assertThat(importService.find(testData.import.project.id, testData.import.author.id!!)).isNull()
  }
}
