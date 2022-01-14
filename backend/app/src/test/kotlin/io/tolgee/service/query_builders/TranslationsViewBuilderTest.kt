package io.tolgee.service.query_builders

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.dtos.request.translation.GetTranslationsParams
import io.tolgee.testing.assertions.Assertions.assertThat
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

@SpringBootTest
class TranslationsViewBuilderTest : AbstractSpringTest() {

  lateinit var testData: TranslationsTestData

  @BeforeMethod
  fun setup() {
    testData = TranslationsTestData()
    testData.generateLotOfData()
    testDataService.saveTestData(testData.root)
  }

  @Test
  fun `returns correct page size and page meta`() {
    val result = TranslationsViewBuilder.getData(
      applicationContext = applicationContext!!,
      projectId = testData.project.id,
      languages = setOf(testData.englishLanguage, testData.germanLanguage),
      PageRequest.of(0, 10),
    )
    assertThat(result.content).hasSize(10)
    assertThat(result.totalElements).isGreaterThan(90)
  }

  @Test
  fun `selects languages`() {
    val result = TranslationsViewBuilder.getData(
      applicationContext = applicationContext!!,
      projectId = testData.project.id,
      languages = setOf(testData.englishLanguage),
      PageRequest.of(
        0, 10,
      )
    )
    assertThat(result.content[1].translations).doesNotContainKey("de")
  }

  @Test
  fun `searches in data`() {
    val result = TranslationsViewBuilder.getData(
      applicationContext = applicationContext!!,
      projectId = testData.project.id,
      languages = setOf(testData.englishLanguage),
      PageRequest.of(0, 10),
      params = GetTranslationsParams().apply {
        search = "A tr"
      }
    )
    assertThat(result.content.first().translations["en"]?.text).isEqualTo("A translation")
    assertThat(result.totalElements).isEqualTo(1)
  }
}
