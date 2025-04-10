package io.tolgee.repository.dataImport

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.dataImport.ImportTestData
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest

@SpringBootTest
class ImportLanguageRepositoryTest : AbstractSpringTest() {
  @Autowired
  lateinit var importLanguageRepository: ImportLanguageRepository

  @Test
  fun `view query returns correct result`() {
    val testData = ImportTestData()
    testData.addFileIssues()
    testDataService.saveTestData(testData.root)
    val result =
      importLanguageRepository
        .findImportLanguagesView(testData.import.id, PageRequest.of(0, 10))
        .content

    assertThat(result).hasSize(3)
    assertThat(result[0].existingLanguageName).isEqualTo("English")
    assertThat(result[0].conflictCount).isEqualTo(3)
    assertThat(result[0].totalCount).isEqualTo(5)
    assertThat(result[0].resolvedCount).isEqualTo(0)
    assertThat(result[0].importFileIssueCount).isEqualTo(4)
  }
}
