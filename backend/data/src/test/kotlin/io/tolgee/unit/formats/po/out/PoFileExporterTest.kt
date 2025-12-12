package io.tolgee.unit.formats.po.out

import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.ExportMessageFormat
import io.tolgee.formats.po.out.PoFileExporter
import io.tolgee.model.ILanguage
import io.tolgee.model.enums.TranslationState
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.ExportFileStructureTemplateProvider
import io.tolgee.service.export.dataProvider.ExportKeyView
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.testing.assert
import io.tolgee.unit.util.assertFile
import io.tolgee.unit.util.getExported
import io.tolgee.util.buildExportTranslationList
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class PoFileExporterTest {
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
      "Plural-Forms: nplurals=3; plural=(n == 1 ? 0 : (n >= 2 && n <= 4) ? 1 : 2)\n"
      "X-Generator: Tolgee\n"

      msgid "key"
      msgid_plural "key"
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
      "Plural-Forms: nplurals=2; plural=(n != 1)\n"
      "X-Generator: Tolgee\n"

      msgid "key"
      msgid_plural "key"
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
      "Plural-Forms: nplurals=2; plural=(n != 1)\n"
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
      "Plural-Forms: nplurals=3; plural=(n == 1 ? 0 : (n >= 2 && n <= 4) ? 1 : 2)\n"
      "X-Generator: Tolgee\n"

      msgid "key"
      msgstr "Ahoj! %s, jak se máš?"

      msgid "key2"
      msgstr "Ahoj! %3${"$"}s, jak se máš?"${"\n"}
      """.trimIndent(),
    )
  }

  @Test
  fun `exports multilines correctly`() {
    val exporter = getWithMultilinesExporter()
    val files = exporter.produceFiles().map { it.key to it.value.bufferedReader().readText() }.toMap()
    val cs = files["cs.po"]
    cs.assert.isEqualTo(
      """
      msgid ""
      msgstr ""
      "Language: cs\n"
      "MIME-Version: 1.0\n"
      "Content-Type: text/plain; charset=UTF-8\n"
      "Content-Transfer-Encoding: 8bit\n"
      "Plural-Forms: nplurals=3; plural=(n == 1 ? 0 : (n >= 2 && n <= 4) ? 1 : 2)\n"
      "X-Generator: Tolgee\n"

      msgid ""
      "I am key\n"
      "Look at me\n"
      "Hello!"
      msgid_plural ""
      "I am key\n"
      "Look at me\n"
      "Hello!"
      msgstr[0] ""
      "%d den\n"
      "newline"
      msgstr[1] "dny"
      msgstr[2] "%d dnů"

      msgid ""
      "I am key\n"
      "Look at me\n"
      "Hello!"
      msgstr ""
      "I am value\n"
      "Look at me\n"
      "Hello!"

      """.trimIndent(),
    )
  }

  @Test
  fun `escapes correctly`() {
    val exporter = getEscapingTestExporter()
    val files = exporter.produceFiles().map { it.key to it.value.bufferedReader().readText() }.toMap()
    val en = files["en.po"]
    en.assert.isEqualTo(
      """
      msgid ""
      msgstr ""
      "Language: en\n"
      "MIME-Version: 1.0\n"
      "Content-Type: text/plain; charset=UTF-8\n"
      "Content-Transfer-Encoding: 8bit\n"
      "Plural-Forms: nplurals=2; plural=(n != 1)\n"
      "X-Generator: Tolgee\n"

      msgid "key"
      msgstr ""
      "\" \n"
      " \\\" \\\\"

      """.trimIndent(),
    )
  }

  @Test
  fun `honors the provided fileStructureTemplate`() {
    val exporter =
      getSimpleExporter(
        params =
          getExportParams().also {
            it.fileStructureTemplate = "{languageTag}/hello/{namespace}.{extension}"
          },
      )
    val files = exporter.produceFiles()
    files["en/hello.po"].assert.isNotNull()
  }

  private fun getSimpleExporter(params: ExportParams = getExportParams()) =
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
      params = params,
    )

  private fun getPluralsExporter() =
    getExporter(
      listOf(
        ExportTranslationView(
          1,
          "{count, plural, one {# day} other {# days}}",
          TranslationState.TRANSLATED,
          ExportKeyView(1, "key", isPlural = true),
          "en",
        ),
        ExportTranslationView(
          1,
          "{count, plural, one {# den} few {dny} other {# dnů}}",
          TranslationState.TRANSLATED,
          ExportKeyView(1, "key", isPlural = true),
          "cs",
        ),
      ),
    )

  private fun getEscapingTestExporter() =
    getExporter(
      listOf(
        ExportTranslationView(
          1,
          "\" \n \\\" \\\\",
          TranslationState.TRANSLATED,
          ExportKeyView(1, "key"),
          "en",
        ),
      ),
    )

  private fun getWithMultilinesExporter() =
    getExporter(
      listOf(
        ExportTranslationView(
          1,
          "{count, plural, one {# den\nnewline} few {dny} other {# dnů}}",
          TranslationState.TRANSLATED,
          ExportKeyView(1, "I am key\nLook at me\nHello!", isPlural = true),
          "cs",
        ),
        ExportTranslationView(
          1,
          "I am value\nLook at me\nHello!",
          TranslationState.TRANSLATED,
          ExportKeyView(1, "I am key\nLook at me\nHello!"),
          "cs",
        ),
      ),
    )

  @Test
  fun `exports with placeholders (ICU placeholders enabled)`() {
    val exporter = getIcuPlaceholdersEnabledExporter()
    val data = getExported(exporter)
    data.assertFile(
      "cs.po",
      """
    |msgid ""
    |msgstr ""
    |"Language: cs\n"
    |"MIME-Version: 1.0\n"
    |"Content-Type: text/plain; charset=UTF-8\n"
    |"Content-Transfer-Encoding: 8bit\n"
    |"Plural-Forms: nplurals=3; plural=(n == 1 ? 0 : (n >= 2 && n <= 4) ? 1 : 2)\n"
    |"X-Generator: Tolgee\n"
    |
    |msgid "key3"
    |msgid_plural "key3"
    |msgstr[0] "%d den %s"
    |msgstr[1] "%d dny"
    |msgstr[2] "%d dní"
    |
    |msgid "item"
    |msgstr "I will be first {icuParam}"
    |
      """.trimMargin(),
    )
  }

  private fun getIcuPlaceholdersEnabledExporter(): PoFileExporter {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "key3",
          text = "{count, plural, one {# den {icuParam}} few {# dny} other {# dní}}",
        ) {
          key.isPlural = true
        }
        add(
          languageTag = "cs",
          keyName = "item",
          text = "I will be first '{'icuParam'}'",
        )
      }
    return getExporter(built.translations, true)
  }

  @Test
  fun `exports with placeholders (ICU placeholders disabled)`() {
    val exporter = getIcuPlaceholdersDisabledExporter()
    val data = getExported(exporter)
    data.assertFile(
      "cs.po",
      """
    |msgid ""
    |msgstr ""
    |"Language: cs\n"
    |"MIME-Version: 1.0\n"
    |"Content-Type: text/plain; charset=UTF-8\n"
    |"Content-Transfer-Encoding: 8bit\n"
    |"Plural-Forms: nplurals=3; plural=(n == 1 ? 0 : (n >= 2 && n <= 4) ? 1 : 2)\n"
    |"X-Generator: Tolgee\n"
    |
    |msgid "key3"
    |msgid_plural "key3"
    |msgstr[0] "# den {icuParam}"
    |msgstr[1] "# dny"
    |msgstr[2] "# dní"
    |
    |msgid "item"
    |msgstr "I will be first {icuParam}"
    |
      """.trimMargin(),
    )
  }

  private fun getIcuPlaceholdersDisabledExporter(): PoFileExporter {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "key3",
          text = "{count, plural, one {'#' den '{'icuParam'}'} few {'#' dny} other {'#' dní}}",
        ) {
          key.isPlural = true
        }
        add(
          languageTag = "cs",
          keyName = "item",
          text = "I will be first {icuParam}",
        )
      }
    return getExporter(built.translations, false)
  }

  private fun getExporter(
    translations: List<ExportTranslationView>,
    isProjectIcuPlaceholdersEnabled: Boolean = true,
    params: ExportParams = getExportParams(),
  ): PoFileExporter {
    val baseLanguageMock = mock<ILanguage>()
    whenever(baseLanguageMock.tag).thenAnswer { "en" }
    return PoFileExporter(
      translations = translations,
      exportParams = params,
      projectIcuPlaceholdersSupport = isProjectIcuPlaceholdersEnabled,
      baseLanguage = baseLanguageMock,
      filePathProvider =
        ExportFilePathProvider(
          template = ExportFileStructureTemplateProvider(params, translations).validateAndGetTemplate(),
          extension = params.format.extension,
        ),
    )
  }

  private fun getExportParams(): ExportParams {
    return ExportParams().also {
      it.messageFormat = ExportMessageFormat.PHP_SPRINTF
      it.format = ExportFormat.PO
    }
  }
}
