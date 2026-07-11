package io.tolgee.unit.formats.apple.out

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.apple.out.AppleXcstringsExporter
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.ExportFileStructureTemplateProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.testing.assert
import io.tolgee.unit.util.assertFile
import io.tolgee.unit.util.getExported
import io.tolgee.util.buildExportTranslationList
import org.junit.jupiter.api.Test

class AppleXcstringsExporterTest {
  @Test
  fun exports() {
    val exporter = getExporter()
    val data = getExported(exporter)

    // Verify sourceLanguage is set from baseLanguageTag
    data["Localizable.xcstrings"]!!.assert.contains("\"sourceLanguage\":\"en\"")
    data["Localizable.xcstrings"]!!.assert.contains("\"version\":\"1.0\"")
    data["Localizable.xcstrings"]!!.assert.contains("\"hello_key\"")
    data["Localizable.xcstrings"]!!.assert.contains("\"en\"")
    data["Localizable.xcstrings"]!!.assert.contains("\"de\"")
    data["Localizable.xcstrings"]!!.assert.contains("\"Hello\"")
    data["Localizable.xcstrings"]!!.assert.contains("\"Hallo\"")
  }

  @Test
  fun `uses baseLanguageTag for sourceLanguage even when base language not exported`() {
    // Export only German translations, but base language is English
    val built =
      buildExportTranslationList {
        add(
          languageTag = "de",
          keyName = "greeting",
          text = "Hallo",
          baseText = "Hello",
        )
      }
    val exporter = getExporter(built.translations, baseLanguageTag = "en-US")
    val data = getExported(exporter)

    // sourceLanguage should be "en-US" even though we only export "de"
    data["Localizable.xcstrings"]!!.assert.contains("\"sourceLanguage\":\"en-US\"")
  }

  @Test
  fun `exports multiple languages with correct sourceLanguage`() {
    val exporter = getMultiLanguageExporter()
    val data = getExported(exporter)

    // Verify sourceLanguage is "en" (the base language)
    data["Localizable.xcstrings"]!!.assert.contains("\"sourceLanguage\":\"en\"")
    // Verify all languages are in localizations
    data["Localizable.xcstrings"]!!.assert.contains("\"en\"")
    data["Localizable.xcstrings"]!!.assert.contains("\"de\"")
    data["Localizable.xcstrings"]!!.assert.contains("\"fr\"")
    data["Localizable.xcstrings"]!!.assert.contains("\"Hello\"")
    data["Localizable.xcstrings"]!!.assert.contains("\"Hallo\"")
    data["Localizable.xcstrings"]!!.assert.contains("\"Bonjour\"")
  }

  @Test
  fun `exports plurals with correct sourceLanguage`() {
    val exporter = getPluralExporter()
    val data = getExported(exporter)

    data["Localizable.xcstrings"]!!.assert.contains("\"sourceLanguage\":\"en\"")
    data["Localizable.xcstrings"]!!.assert.contains("\"variations\"")
    data["Localizable.xcstrings"]!!.assert.contains("\"plural\"")
    // Verify plural forms
    data["Localizable.xcstrings"]!!.assert.contains("\"one\"")
    data["Localizable.xcstrings"]!!.assert.contains("\"other\"")
  }

  private fun getExporter(params: ExportParams = getExportParams()): AppleXcstringsExporter {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "de",
          keyName = "hello_key",
          text = "Hallo",
          baseText = "Hello",
        )
        add(
          languageTag = "en",
          keyName = "hello_key",
          text = "Hello",
        )
      }
    return getExporter(built.translations, params = params)
  }

  private fun getMultiLanguageExporter(params: ExportParams = getExportParams()): AppleXcstringsExporter {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "de",
          keyName = "greeting",
          text = "Hallo",
          baseText = "Hello",
        )
        add(
          languageTag = "fr",
          keyName = "greeting",
          text = "Bonjour",
          baseText = "Hello",
        )
        add(
          languageTag = "en",
          keyName = "greeting",
          text = "Hello",
        )
      }
    return getExporter(built.translations, params = params)
  }

  private fun getPluralExporter(params: ExportParams = getExportParams()): AppleXcstringsExporter {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "en",
          keyName = "items_count",
          text = "{count, plural, one {# item} other {# items}}",
        ) {
          key.isPlural = true
        }
        add(
          languageTag = "de",
          keyName = "items_count",
          text = "{count, plural, one {# Artikel} other {# Artikel}}",
          baseText = "{count, plural, one {# item} other {# items}}",
        ) {
          key.isPlural = true
        }
      }
    return getExporter(built.translations, params = params)
  }

  private fun getExporter(
    translations: List<ExportTranslationView>,
    isProjectIcuPlaceholdersEnabled: Boolean = true,
    baseLanguageTag: String = "en",
    params: ExportParams = getExportParams(),
  ): AppleXcstringsExporter {
    return AppleXcstringsExporter(
      translations = translations,
      exportParams = params,
      objectMapper = jacksonObjectMapper(),
      isProjectIcuPlaceholdersEnabled = isProjectIcuPlaceholdersEnabled,
      filePathProvider =
        ExportFilePathProvider(
          template = ExportFileStructureTemplateProvider(params, translations).validateAndGetTemplate(),
          extension = params.format.extension,
        ),
      baseLanguageTag = baseLanguageTag,
    )
  }

  private fun getExportParams(): ExportParams {
    return ExportParams().also { it.format = ExportFormat.APPLE_XCSTRINGS }
  }
}
