package io.tolgee.unit.formats.apple.out

import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.apple.out.AppleStringsStringsdictExporter
import io.tolgee.model.enums.TranslationState
import io.tolgee.service.export.dataProvider.ExportKeyView
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.unit.util.assertFile
import io.tolgee.unit.util.getExported
import org.junit.jupiter.api.Test

class AppleStringsIntegerPlaceholdersTest {

  @Test
  fun `preserves integer placeholders during export`() {
    val exporter = getExporter()
    val data = getExported(exporter)

    data.assertFile(
      "en.lproj/Localizable.strings",
      """
      |"test_placeholder_i" = "placeholder i: %i";
      |
      |"test_placeholder_d" = "placeholder d: %d";
      |
      |"test_combined" = "This combines %d and %i with 100%% certainty";
      |
      |
      """.trimMargin(),
    )
  }

  private fun getExporter(): AppleStringsStringsdictExporter {
    val translations = listOf(
      ExportTranslationView(
        1,
        "placeholder i: %i",
        TranslationState.TRANSLATED,
        ExportKeyView(1, "test_placeholder_i"),
        "en",
      ),
      ExportTranslationView(
        2,
        "placeholder d: %d",
        TranslationState.TRANSLATED,
        ExportKeyView(2, "test_placeholder_d"),
        "en",
      ),
      ExportTranslationView(
        3,
        "This combines %d and %i with 100% certainty",
        TranslationState.TRANSLATED,
        ExportKeyView(3, "test_combined"),
        "en",
      ),
    )

    return AppleStringsStringsdictExporter(
      translations = translations,
      exportParams = getExportParams(),
    )
  }

  private fun getExportParams() = ExportParams().also { it.format = ExportFormat.APPLE_STRINGS_STRINGSDICT }
}