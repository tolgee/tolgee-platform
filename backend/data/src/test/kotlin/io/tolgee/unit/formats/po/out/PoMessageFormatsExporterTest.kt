package io.tolgee.unit.formats.po.out

import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.po.PoSupportedMessageFormat
import io.tolgee.formats.po.out.PoFileExporter
import io.tolgee.model.ILanguage
import io.tolgee.unit.util.assertFile
import io.tolgee.unit.util.getExported
import io.tolgee.util.buildExportTranslationList
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class PoMessageFormatsExporterTest {
  @Test
  fun php() {
    val exporter = getExporter(PoSupportedMessageFormat.PHP)
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
    |"Plural-Forms: nplurals = 3; plural = (n === 1 ? 0 : (n >= 2 && n <= 4) ? 1 : 2)\n"
    |"X-Generator: Tolgee\n"
    |
    |msgid "key3"
    |msgstr "%3${'$'}d %2${'$'}s %1${'$'}s"
    |
      """.trimMargin(),
    )
  }

//  @Test
//  fun python() {
//    val exporter = getExporter(PoSupportedMessageFormat.PYTHON)
//    val data = getExported(exporter)
//    data.assertFile(
//      "cs.po",
//      """
//    |msgid ""
//    |msgstr ""
//    |"Language: cs\n"
//    |"MIME-Version: 1.0\n"
//    |"Content-Type: text/plain; charset=UTF-8\n"
//    |"Content-Transfer-Encoding: 8bit\n"
//    |"Plural-Forms: nplurals = 3; plural = (n === 1 ? 0 : (n >= 2 && n <= 4) ? 1 : 2)\n"
//    |"X-Generator: Tolgee\n"
//    |
//    |msgid "key3"
//    |msgstr "%(2)d %(1)s %(0)s"
//    |
//      """.trimMargin(),
//    )
//  }

  @Test
  fun c() {
    val exporter = getExporter(PoSupportedMessageFormat.C)
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
    |"Plural-Forms: nplurals = 3; plural = (n === 1 ? 0 : (n >= 2 && n <= 4) ? 1 : 2)\n"
    |"X-Generator: Tolgee\n"
    |
    |msgid "key3"
    |msgstr "%d %s %s"
    |
      """.trimMargin(),
    )
  }

  private fun getExporter(poSupportedMessageFormat: PoSupportedMessageFormat): PoFileExporter {
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
    return PoFileExporter(
      translations = built.translations,
      exportParams = ExportParams(),
      baseLanguage = baseLanguageMock,
      baseTranslationsProvider = { listOf() },
      poSupportedMessageFormat = poSupportedMessageFormat,
    )
  }
}
