package io.tolgee.service.query_builders

import io.tolgee.AbstractSpringTest
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.dtos.request.GetTranslationsParams
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
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
  fun `sorts data correctly by de text`() {
    val pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("translations.de.text")))

    val result = TranslationsViewBuilder.getData(
      applicationContext = applicationContext!!,
      projectId = testData.project.id,
      languages = setOf(testData.englishLanguage, testData.germanLanguage),
      pageRequest
    )
    assertThat(result.content.first().translations["de"]?.text).isEqualTo("Z translation")
  }

  @Test
  fun `sorts data correctly by en text`() {
    val pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("translations.en.text")))

    val result = TranslationsViewBuilder.getData(
      applicationContext = applicationContext!!,
      projectId = testData.project.id,
      languages = setOf(testData.englishLanguage),
      pageRequest,
    )
    assertThat(result.content[1].translations["en"]?.text).isEqualTo("A translation")
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
      params = GetTranslationsParams(search = "A tr")
    )
    assertThat(result.content.first().translations["en"]?.text).isEqualTo("A translation")
    assertThat(result.totalElements).isEqualTo(1)
  }
}
