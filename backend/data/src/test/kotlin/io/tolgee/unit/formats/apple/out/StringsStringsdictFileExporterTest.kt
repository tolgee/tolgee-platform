package io.tolgee.unit.formats.apple.out

import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.apple.out.AppleStringsStringsdictExporter
import io.tolgee.model.ILanguage
import io.tolgee.model.enums.TranslationState
import io.tolgee.service.export.dataProvider.ExportKeyView
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class StringsStringsdictFileExporterTest {
  @Test
  fun `exports`() {
    val exporter = getExporter()

    val files = exporter.produceFiles()
    val data = files.map { it.key to it.value.bufferedReader().readText() }.toMap()

    // generate this with:
    // data.map { "data.assertFile(\"${it.key}\", \"\"\"\n    |${it.value.replace("\$", "\${'$'}").replace("\n", "\n    |")}\n    \"\"\".trimMargin())" }.joinToString("\n")
    data.assertFile(
      "en.lproj/Localizable.strings",
      """
    |"key" = "Hello! I am great today! There you have some params %d, %@, %e, %f";
    |
      """.trimMargin(),
    )
    data.assertFile(
      "en.lproj/Localizable.stringsdict",
      """
    |<?xml version="1.0" encoding="UTF-8"?>
    |
    |<plist version="1.0">
    |  <dict>
    |    <key>key</key>
    |    <dict>
    |      <key>NSStringLocalizedFormatKey</key>
    |      <string>%#${'$'}{#@key@}</string>
    |      <key>key</key>
    |      <dict>
    |        <key>one</key>
    |        <string>%li day</string>
    |        <key>other</key>
    |        <string>%li days</string>
    |      </dict>
    |    </dict>
    |  </dict>
    |</plist>
    |
      """.trimMargin(),
    )
    data.assertFile(
      "homepage/en.lproj/Localizable.strings",
      """
    |"key" = "Namespaced";
    |
      """.trimMargin(),
    )
    data.assertFile(
      "homepage/en.lproj/Localizable.stringsdict",
      """
    |<?xml version="1.0" encoding="UTF-8"?>
    |
    |<plist version="1.0">
    |  <dict>
    |    <key>key</key>
    |    <dict>
    |      <key>NSStringLocalizedFormatKey</key>
    |      <string>%#${'$'}{#@key@}</string>
    |      <key>key</key>
    |      <dict>
    |        <key>one</key>
    |        <string>%li day</string>
    |        <key>other</key>
    |        <string>%li days</string>
    |      </dict>
    |    </dict>
    |  </dict>
    |</plist>
    |
      """.trimMargin(),
    )
    data.assertFile(
      "homepage/cs.lproj/Localizable.strings",
      """
    |"key" = "Namespaced";
    |
      """.trimMargin(),
    )
    data.assertFile(
      "cs.lproj/Localizable.stringsdict",
      """
    |<?xml version="1.0" encoding="UTF-8"?>
    |
    |<plist version="1.0">
    |  <dict>
    |    <key>key</key>
    |    <dict>
    |      <key>NSStringLocalizedFormatKey</key>
    |      <string>%#${'$'}{#@key@}</string>
    |      <key>key</key>
    |      <dict>
    |        <key>one</key>
    |        <string>%li den</string>
    |        <key>few</key>
    |        <string>dny</string>
    |        <key>other</key>
    |        <string>%li dnů</string>
    |      </dict>
    |    </dict>
    |  </dict>
    |</plist>
    |
      """.trimMargin(),
    )
  }

  private fun Map<String, String>.assertFile(
    file: String,
    content: String,
  ) {
    this[file]!!.assert.isEqualTo(content)
  }

  private fun getExporter() =
    getExporter(
      listOf(
        ExportTranslationView(
          1,
          "Hello! I am great today! There you have some params " +
            "{number, number}, {name}, {number, number, scientific}, " +
            "{number, number, 0.000000}",
          TranslationState.TRANSLATED,
          ExportKeyView(1, "key"),
          "en",
        ),
        ExportTranslationView(
          1,
          "Namespaced",
          TranslationState.TRANSLATED,
          ExportKeyView(1, "key", namespace = "homepage"),
          "en",
        ),
        ExportTranslationView(
          1,
          "{count, plural, one {# day} other {# days}}",
          TranslationState.TRANSLATED,
          ExportKeyView(1, "key", namespace = "homepage"),
          "en",
        ),
        ExportTranslationView(
          1,
          "Namespaced",
          TranslationState.TRANSLATED,
          ExportKeyView(1, "key", namespace = "homepage"),
          "cs",
        ),
        ExportTranslationView(
          1,
          "{count, plural, one {# day} other {# days}}",
          TranslationState.TRANSLATED,
          ExportKeyView(1, "key"),
          "en",
        ),
        ExportTranslationView(
          1,
          "{count, plural, one {# den} few {dny} other {# dnů}}",
          TranslationState.TRANSLATED,
          ExportKeyView(1, "key"),
          "cs",
        ),
      ),
    )

  private fun getExporter(translations: List<ExportTranslationView>): AppleStringsStringsdictExporter {
    val baseLanguageMock = mock<ILanguage>()
    whenever(baseLanguageMock.tag).thenAnswer { "en" }

    return AppleStringsStringsdictExporter(
      translations = translations,
      exportParams = ExportParams(),
    )
  }
}
