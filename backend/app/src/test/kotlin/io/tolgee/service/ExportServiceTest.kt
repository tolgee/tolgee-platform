package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.model.enums.TranslationState
import io.tolgee.service.export.dataProvider.ExportDataProvider
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class ExportServiceTest : AbstractSpringTest() {
  @Test
  fun `returns correct export data`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)
    val exportParams = ExportParams(filterState = null)

    val provider = ExportDataProvider(entityManager, exportParams, testData.project.id)

    val result = provider.getData()

    assertThat(result).hasSize(4)
  }

  @Test
  fun `returns empty translations correct export data`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)
    val exportParams = ExportParams(filterState = listOf(TranslationState.UNTRANSLATED))

    val provider = ExportDataProvider(entityManager, exportParams, testData.project.id)

    val result = provider.getData()

    assertThat(result).hasSize(2)
    assertThat(result).allMatch { it.id == null }
  }

  @Test
  fun `selects languages for export data`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)

    val exportParams = ExportParams(languages = setOf("de"))
    val provider = ExportDataProvider(entityManager, exportParams, testData.project.id)
    val result = provider.getData()

    assertThat(result).hasSize(1)
    assertThat(result[0].languageTag).isEqualTo("de")
  }

  @Test
  fun `filters keyIdIn for export data`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)

    val exportParams = ExportParams(filterKeyId = listOf(testData.aKey.id))

    val provider = ExportDataProvider(entityManager, exportParams, testData.project.id)
    val result = provider.getData()

    assertThat(result).hasSize(1)
    assertThat(result[0].key.id).isEqualTo(testData.aKey.id)
  }

  @Test
  fun `filters keyNotIdIn for export data`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)

    val exportParams = ExportParams(filterKeyIdNot = listOf(testData.aKey.id))
    val provider = ExportDataProvider(entityManager, exportParams, testData.project.id)
    val result = provider.getData()

    assertThat(result).hasSize(1)
    assertThat(result[0].key.id).isNotEqualTo(testData.aKey.id)
  }

  @Test
  fun `filters export data by tag`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)

    val tag = "Cool tag"
    val exportParams = ExportParams(filterTag = tag)
    val provider = ExportDataProvider(entityManager, exportParams, testData.project.id)
    val result = provider.getData()

    assertThat(result).hasSize(1)

    val key = keyService.get(result[0].key.id)
    assertThat(key.keyMeta?.tags?.toList()?.first()?.name).isEqualTo(tag)
  }

  @Test
  fun `filters export data by key prefix`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)

    val exportParams = ExportParams(filterKeyPrefix = "A")
    val provider = ExportDataProvider(entityManager, exportParams, testData.project.id)
    val result = provider.getData()

    assertThat(result).hasSize(1)
    assertThat(result[0].key.id).isEqualTo(testData.aKey.id)
  }

  @Test
  fun `filters export data by state`() {
    val testData = TranslationsTestData()
    testData.addTranslationsWithStates()
    testDataService.saveTestData(testData.root)

    val exportParams = ExportParams(filterState = listOf(TranslationState.REVIEWED))
    val provider = ExportDataProvider(entityManager, exportParams, testData.project.id)
    val result = provider.getData()

    assertThat(result).hasSize(5)
    assertThat(result.map { it.state }).allMatch { it == TranslationState.REVIEWED }
  }
}
