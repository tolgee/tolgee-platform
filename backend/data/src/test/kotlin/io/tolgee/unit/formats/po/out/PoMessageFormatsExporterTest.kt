package io.tolgee.unit.formats.po.out

import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.ExportMessageFormat
import io.tolgee.formats.po.out.PoFileExporter
import io.tolgee.model.ILanguage
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.unit.util.assertFile
import io.tolgee.unit.util.getExported
import io.tolgee.util.buildExportTranslationList
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class PoMessageFormatsExporterTest {
  @Test
  fun php() {
    val exporter = getExporter(ExportMessageFormat.PHP_SPRINTF)
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
    |msgstr "%3${'$'}d %2${'$'}s %1${'$'}s"
    |
      """.trimMargin(),
    )
  }

  @Test
  fun python() {
    val exporter = getExporter(ExportMessageFormat.PYTHON_PERCENT)
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
    |msgstr "%(2)d %(1)s %(0)s"
    |
      """.trimMargin(),
    )
  }

  @Test
  fun c() {
    val exporter = getExporter(ExportMessageFormat.C_SPRINTF)
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
    |msgstr "%3${'$'}d %2${'$'}s %1${'$'}s"
    |
      """.trimMargin(),
    )
  }

  private fun getExporter(importFormat: ExportMessageFormat): PoFileExporter {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "key3",
          text = "{2, number} {1} {0}",
        )
      }

    val baseLanguageMock = mock<ILanguage>()
    whenever(baseLanguageMock.tag).thenAnswer { "en" }
    val params =
      ExportParams().also {
        it.messageFormat = importFormat
      }
    return PoFileExporter(
      translations = built.translations,
      exportParams = params,
      baseLanguage = baseLanguageMock,
      projectIcuPlaceholdersSupport = true,
      filePathProvider =
        ExportFilePathProvider(
          template = "{languageTag}.{extension}",
          extension = "po",
        ),
    )
  }
}
