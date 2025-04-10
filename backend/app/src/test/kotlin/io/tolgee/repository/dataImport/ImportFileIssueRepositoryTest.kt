package io.tolgee.repository.dataImport

import io.tolgee.AbstractSpringTest
import io.tolgee.Application
import io.tolgee.development.testDataBuilder.data.dataImport.ImportTestData
import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType
import io.tolgee.repository.dataImport.issues.ImportFileIssueRepository
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(classes = [Application::class])
@Transactional
class ImportFileIssueRepositoryTest : AbstractSpringTest() {
  @Autowired
  lateinit var importFileIssueRepository: ImportFileIssueRepository

  @Test
  fun `view query returns correct result`() {
    val testData = ImportTestData()
    testData.addFileIssues()
    testDataService.saveTestData(testData.root)
    val result =
      importFileIssueRepository
        .findAllByFileIdView(
          testData.importBuilder.data.importFiles[0]
            .self.id,
          PageRequest.of(0, 10),
        ).content

    assertThat(result).hasSize(4)
    assertThat(result[0].params).hasSize(1)
    assertThat(result[1].params).hasSize(2)
    assertThat(result[2].params).hasSize(1)
    assertThat(result[3].params).hasSize(3)
    assertThat(result[3].params[0].type).isEqualTo(FileIssueParamType.KEY_NAME)
    assertThat(result[3].params[0].value).isEqualTo("value_is_not_string_key")
    assertThat(result[3].params[1].value).isEqualTo("5")
    assertThat(result[3].params[2].value).isEqualTo("1")
  }
}
