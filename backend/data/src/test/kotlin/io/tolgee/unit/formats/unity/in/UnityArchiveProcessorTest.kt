package io.tolgee.unit.formats.unity.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.unity.`in`.UnityArchiveProcessor
import io.tolgee.formats.unity.`in`.UnityCollectionImportModel
import io.tolgee.formats.unity.out.UnityExporter
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.ExportFileStructureTemplateProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.testing.assert
import io.tolgee.util.buildExportTranslationList
import org.junit.jupiter.api.Test

class UnityArchiveProcessorTest {
  private val objectMapper = jacksonObjectMapper()
  private val processor = UnityArchiveProcessor(objectMapper, ObjectMapper(YAMLFactory()))

  @Test
  fun `detects a unity collection and ignores non-unity archives`() {
    processor.isUnity(exportFiles(simple())).assert.isEqualTo(true)
    processor.isUnity(listOf(ImportFileDto("messages.json", "{}".toByteArray()))).assert.isEqualTo(false)
  }

  @Test
  fun `round-trips keys, values and the smart flag through the synthetic model`() {
    val model = merge(exportFiles(simple()))

    model.collectionName.assert.isEqualTo("Localization")
    val greeting = model.keys.first { it.name == "greeting" }
    greeting.isSmart.assert.isEqualTo(false)
    greeting.translations
      .first { it.locale == "en" }
      .value.assert
      .isEqualTo("Hello")

    val welcome = model.keys.first { it.name == "welcome" }
    welcome.isSmart.assert.isEqualTo(true)
    welcome.translations
      .first { it.locale == "en" }
      .value.assert
      .isEqualTo("Hi {name}")
  }

  @Test
  fun `splits a smart plural back into a keyword form map`() {
    val built =
      buildExportTranslationList {
        add(languageTag = "en", keyName = "apples", text = "{count, plural, one {# apple} other {# apples}}") {
          key.isPlural = true
        }
      }
    val model = merge(exportFiles(built.translations))
    val forms =
      model.keys
        .first { it.name == "apples" }
        .translations
        .first { it.locale == "en" }
        .pluralForms!!
    forms["one"].assert.isEqualTo("{} apple")
    forms["other"].assert.isEqualTo("{} apples")
  }

  @Test
  fun `passes non-unity files through while merging unity ones`() {
    val files = exportFiles(simple()) + ImportFileDto("messages.json", "{}".toByteArray())
    val result = processor.merge(files)
    result.map { it.name }.assert.contains("messages.json")
    result.count { it.name.endsWith(".unity") }.assert.isEqualTo(1)
    // .asset and .meta files are consumed by the merge
    result.none { it.name.endsWith(".asset") }.assert.isEqualTo(true)
  }

  private fun merge(files: List<ImportFileDto>): UnityCollectionImportModel {
    val synthetic = processor.merge(files).first { it.name.endsWith(".unity") }
    return objectMapper.readValue(synthetic.data)
  }

  private fun simple(): List<ExportTranslationView> {
    return buildExportTranslationList {
      add(languageTag = "en", keyName = "greeting", text = "Hello")
      add(languageTag = "en", keyName = "welcome", text = "Hi {name}")
    }.translations
  }

  private fun exportFiles(translations: List<ExportTranslationView>): List<ImportFileDto> {
    val params = ExportParams().also { it.format = ExportFormat.UNITY }
    val exporter =
      UnityExporter(
        translations = translations,
        exportParams = params,
        isProjectIcuPlaceholdersEnabled = true,
        filePathProvider =
          ExportFilePathProvider(
            template = ExportFileStructureTemplateProvider(params, translations).validateAndGetTemplate(),
            extension = params.format.extension,
          ),
      )
    return exporter.produceFiles().map { ImportFileDto(it.key, it.value.readBytes()) }
  }
}
