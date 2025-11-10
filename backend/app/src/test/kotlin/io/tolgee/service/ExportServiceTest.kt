package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.NamespacesTestData
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.enums.TranslationState
import io.tolgee.service.export.ExportService
import io.tolgee.service.export.dataProvider.ExportDataProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class ExportServiceTest : AbstractSpringTest() {
  @Autowired
  private lateinit var exportService: ExportService

  @Test
  fun `returns correct export data`() {
    val testData = TranslationsTestData()

    testDataService.saveTestData(testData.root)
    val exportParams = ExportParams(filterState = null)

    val provider = ExportDataProvider(applicationContext, exportParams, testData.project.id)

    val result = provider.data

    assertThat(result).hasSize(4)
  }

  @Test
  fun `returns empty translations correct export data`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)
    val exportParams = ExportParams(filterState = listOf(TranslationState.UNTRANSLATED))

    val provider = ExportDataProvider(applicationContext, exportParams, testData.project.id)

    val result = provider.data

    assertThat(result).hasSize(2)
    assertThat(result).allMatch { it.id == null }
  }

  @Test
  fun `selects languages for export data`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)

    val exportParams = ExportParams(languages = setOf("de"))
    val provider = ExportDataProvider(applicationContext, exportParams, testData.project.id)
    val result = provider.data

    assertThat(result).hasSize(1)
    assertThat(result[0].languageTag).isEqualTo("de")
  }

  @Test
  fun `filters keyIdIn for export data`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)

    val exportParams = ExportParams(filterKeyId = listOf(testData.aKey.id))

    val provider = ExportDataProvider(applicationContext, exportParams, testData.project.id)
    val result = provider.data

    assertThat(result).hasSize(1)
    assertThat(result[0].key.id).isEqualTo(testData.aKey.id)
  }

  @Test
  fun `filters keyNotIdIn for export data`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)

    val exportParams = ExportParams(filterKeyIdNot = listOf(testData.aKey.id))
    val provider = ExportDataProvider(applicationContext, exportParams, testData.project.id)
    val result = provider.data

    assertThat(result).hasSize(1)
    assertThat(result[0].key.id).isNotEqualTo(testData.aKey.id)
  }

  @Test
  fun `filters export data by tag`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)

    val tag = "Cool tag"
    val exportParams = ExportParams(filterTag = tag)
    val provider = ExportDataProvider(applicationContext, exportParams, testData.project.id)
    val result = provider.data

    assertThat(result).hasSize(1)

    val key = keyService.get(result[0].key.id)
    assertThat(
      key.keyMeta
        ?.tags
        ?.toList()
        ?.first()
        ?.name,
    ).isEqualTo(tag)
  }

  @Test
  fun `filters export data by tag in`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)

    var result = getResultFilteredByTagIn(testData, listOf("Cool tag", "Lame tag"))

    assertThat(result).hasSize(2)

    result = getResultFilteredByTagIn(testData, listOf("Lame tag"))

    assertThat(result).hasSize(1)
  }

  @Test
  fun `filters export data by tag not in`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)

    var result = getResultFilteredByTagNotIn(testData, listOf("Cool tag", "Lame tag"))

    assertThat(result).hasSize(0)

    result = getResultFilteredByTagNotIn(testData, listOf("Lame tag"))

    assertThat(result).hasSize(1)
  }

  private fun getResultFilteredByTagIn(
    testData: TranslationsTestData,
    tags: List<String>,
  ): List<ExportTranslationView> {
    val exportParams = ExportParams(filterTagIn = tags)
    val provider = ExportDataProvider(applicationContext, exportParams, testData.project.id)
    val result = provider.data
    return result
  }

  private fun getResultFilteredByTagNotIn(
    testData: TranslationsTestData,
    tags: List<String>,
  ): List<ExportTranslationView> {
    val exportParams = ExportParams(filterTagNotIn = tags)
    val provider = ExportDataProvider(applicationContext, exportParams, testData.project.id)
    return provider.data
  }

  @Test
  fun `filters export data by key prefix`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)

    val exportParams = ExportParams(filterKeyPrefix = "A")
    val provider = ExportDataProvider(applicationContext, exportParams, testData.project.id)
    val result = provider.data

    assertThat(result).hasSize(1)
    assertThat(result[0].key.id).isEqualTo(testData.aKey.id)
  }

  @Test
  fun `filters export data by state`() {
    val testData = TranslationsTestData()
    testData.addTranslationsWithStates()
    testDataService.saveTestData(testData.root)

    val exportParams = ExportParams(filterState = listOf(TranslationState.REVIEWED))
    val provider = ExportDataProvider(applicationContext, exportParams, testData.project.id)
    val result = provider.data

    assertThat(result).hasSize(5)
    assertThat(result.map { it.state }).allMatch { it == TranslationState.REVIEWED }
  }

  @Test
  fun `filters export data by namespace`() {
    val testData = NamespacesTestData()
    testDataService.saveTestData(testData.root)

    val exportParams = ExportParams(filterNamespace = listOf("ns-1", null))
    val provider = ExportDataProvider(applicationContext, exportParams, testData.projectBuilder.self.id)
    val result = provider.data

    result.assert.hasSize(4)
    result.forEach {
      it.key.namespace.assert
        .isIn(null, "ns-1")
    }
  }

  @Test
  fun `export with 2 namespaces and no {namespace} in template path should throw`() {
    val testData = TranslationsTestData()
    testData.addTwoNamespacesTranslations()
    testData.addFewKeysWithTags()
    testDataService.saveTestData(testData.root)

    val exportParams =
      ExportParams(fileStructureTemplate = "{languageTag}.{extension}")

    assertThatThrownBy {
      exportService.export(testData.project.id, exportParams)
    }.isInstanceOf(BadRequestException::class.java)
  }

  @Test
  fun `export with 2 namespaces and {namespace} in template path should not throw`() {
    val testData = TranslationsTestData()
    testData.addTwoNamespacesTranslations()
    testDataService.saveTestData(testData.root)

    val exportParams =
      ExportParams(fileStructureTemplate = "{namespace}/{languageTag}.{extension}")
    exportService.export(testData.project.id, exportParams)
  }
}
