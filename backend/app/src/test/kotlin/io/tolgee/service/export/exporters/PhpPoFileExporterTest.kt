package io.tolgee.service.export.exporters

import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.po.SupportedFormat
import io.tolgee.model.ILanguage
import io.tolgee.model.enums.TranslationState
import io.tolgee.service.export.dataProvider.ExportKeyView
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class PhpPoFileExporterTest {
  @Test
  fun `exports plurals correctly`() {
    val exporter = getPluralsExporter()

    val files = exporter.produceFiles().map { it.key to it.value.bufferedReader().readText() }.toMap()
    files["cs.po"].assert.isEqualTo(
      """
      msgid ""
      msgstr ""
      "Language: cs\n"
      "MIME-Version: 1.0\n"
      "Content-Type: text/plain; charset=UTF-8\n"
      "Content-Transfer-Encoding: 8bit\n"
      "Plural-Forms: nplurals = 3; plural = (n === 1 ? 0 : (n >= 2 && n <= 4) ? 1 : 2)\n"
      "X-Generator: Tolgee\n"
      
      msgid "key"
      msgstr[0] "%d den"
      msgstr[1] "dny"
      msgstr[2] "%d dnů"${"\n"}
      """.trimIndent(),
    )
    files["en.po"].assert.isEqualTo(
      """
      msgid ""
      msgstr ""
      "Language: en\n"
      "MIME-Version: 1.0\n"
      "Content-Type: text/plain; charset=UTF-8\n"
      "Content-Transfer-Encoding: 8bit\n"
      "Plural-Forms: nplurals = 2; plural = (n !== 1)\n"
      "X-Generator: Tolgee\n"
      
      msgid "key"
      msgstr[0] "%d day"
      msgstr[1] "%d days"${"\n"}
      """.trimIndent(),
    )
  }

  @Test
  fun `exports simple`() {
    val exporter = getSimpleExporter()
    val files = exporter.produceFiles().map { it.key to it.value.bufferedReader().readText() }.toMap()
    files["en.po"].assert.isEqualTo(
      """
      msgid ""
      msgstr ""
      "Language: en\n"
      "MIME-Version: 1.0\n"
      "Content-Type: text/plain; charset=UTF-8\n"
      "Content-Transfer-Encoding: 8bit\n"
      "Plural-Forms: nplurals = 2; plural = (n !== 1)\n"
      "X-Generator: Tolgee\n"
      
      msgid "key"
      msgstr "Hello! %s, how are you?"${"\n"}
      """.trimIndent(),
    )

    files["cs.po"].assert.isEqualTo(
      """
      msgid ""
      msgstr ""
      "Language: cs\n"
      "MIME-Version: 1.0\n"
      "Content-Type: text/plain; charset=UTF-8\n"
      "Content-Transfer-Encoding: 8bit\n"
      "Plural-Forms: nplurals = 3; plural = (n === 1 ? 0 : (n >= 2 && n <= 4) ? 1 : 2)\n"
      "X-Generator: Tolgee\n"

      msgid "key"
      msgstr "Ahoj! %s, jak se máš?"

      msgid "key2"
      msgstr "Ahoj! %3${"$"}s, jak se máš?"${"\n"}
      """.trimIndent(),
    )
  }

  private fun getSimpleExporter() =
    getExporter(
      listOf(
        ExportTranslationView(
          1,
          "Hello! {name}, how are you?",
          TranslationState.TRANSLATED,
          ExportKeyView(1, "key"),
          "en",
        ),
        ExportTranslationView(
          1,
          "Ahoj! {0}, jak se máš?",
          TranslationState.TRANSLATED,
          ExportKeyView(1, "key"),
          "cs",
        ),
        ExportTranslationView(
          1,
          "Ahoj! {2}, jak se máš?",
          TranslationState.TRANSLATED,
          ExportKeyView(1, "key2"),
          "cs",
        ),
      ),
    )

  private fun getPluralsExporter() =
    getExporter(
      listOf(
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

  private fun getExporter(translations: List<ExportTranslationView>): PoFileExporter {
    val baseLanguageMock = mock<ILanguage>()
    whenever(baseLanguageMock.tag).thenAnswer { "en" }

    return PoFileExporter(
      translations = translations,
      exportParams = ExportParams(),
      baseTranslationsProvider = { listOf() },
      baseLanguageMock,
      SupportedFormat.PHP,
    )
  }
}
