package io.tolgee.repository

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.dtos.request.export.ExportParamsNull
import io.tolgee.model.enums.TranslationState
import io.tolgee.testing.assertions.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import org.testng.annotations.Test

@SpringBootTest
@Transactional
class TranslationRepositoryTest : AbstractSpringTest() {

  @Autowired
  lateinit var translationRepository: TranslationRepository

  @Test
  fun `returns correct export data`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)
    val exportParams = ExportParams()
    val result = translationRepository.getDataForExport(
      testData.project.id,
      exportParams,
      ExportParamsNull(exportParams)
    )
    assertThat(result).hasSize(2)
  }

  @Test
  fun `selects languages for export data`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)

    val exportParams = ExportParams(languages = setOf("de"))
    val result = translationRepository.getDataForExport(
      testData.project.id,
      exportParams,
      ExportParamsNull(exportParams)
    )

    assertThat(result).hasSize(1)
    assertThat(result[0].language.tag).isEqualTo("de")
  }

  @Test
  fun `filters keyIdIn for export data`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)

    val exportParams = ExportParams(filterKeyId = listOf(testData.aKey.id))
    val result = translationRepository.getDataForExport(
      testData.project.id,
      exportParams,
      ExportParamsNull(exportParams)
    )
    assertThat(result).hasSize(1)
    assertThat(result[0].key.id).isEqualTo(testData.aKey.id)
  }

  @Test
  fun `filters keyNotIdIn for export data`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)

    val exportParams = ExportParams(filterKeyIdNot = listOf(testData.aKey.id))
    val result = translationRepository.getDataForExport(
      testData.project.id,
      exportParams,
      ExportParamsNull(exportParams)
    )

    assertThat(result).hasSize(1)
    assertThat(result[0].key.id).isNotEqualTo(testData.aKey.id)
  }

  @Test
  fun `filters export data by tag`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)

    val tag = "Cool tag"

    val exportParams = ExportParams(filterTag = tag)
    val result = translationRepository.getDataForExport(
      testData.project.id,
      exportParams,
      ExportParamsNull(exportParams)
    )
    assertThat(result).hasSize(1)
    assertThat(result[0].key.keyMeta?.tags?.toList()?.first()?.name).isEqualTo(tag)
  }

  @Test
  fun `filters export data by key prefix`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)

    val exportParams = ExportParams(filterKeyPrefix = "A")
    val result = translationRepository.getDataForExport(
      testData.project.id,
      exportParams,
      ExportParamsNull(exportParams)
    )

    assertThat(result).hasSize(1)
    assertThat(result[0].key.id).isEqualTo(testData.aKey.id)
  }

  @Test
  fun `filters export data by state`() {
    val testData = TranslationsTestData()
    testData.addTranslationsWithStates()
    testDataService.saveTestData(testData.root)

    val exportParams = ExportParams(filterState = listOf(TranslationState.REVIEWED))
    val result = translationRepository.getDataForExport(
      testData.project.id,
      exportParams,
      ExportParamsNull(exportParams)
    )

    assertThat(result).hasSize(5)
    assertThat(result.map { it.state }).allMatch { it == TranslationState.REVIEWED }
  }

  @Test
  fun `filters export data by not state`() {
    val testData = TranslationsTestData()
    testData.addTranslationsWithStates()
    testDataService.saveTestData(testData.root)

    val exportParams = ExportParams(filterStateNot = listOf(TranslationState.REVIEWED))
    val result = translationRepository.getDataForExport(
      testData.project.id,
      exportParams,
      ExportParamsNull(exportParams)
    )

    assertThat(result).hasSizeGreaterThan(0)
    assertThat(result.map { it.state }).allMatch { it != TranslationState.REVIEWED }
  }
}
