package io.tolgee.unit.formats.apple.out

import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.apple.out.AppleStringsStringsdictExporter
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

class StringsStringsdictFileExporterTest {
  @Test
  fun exports() {
    val exporter = getExporter()

    val data = getExported(exporter)

    // generate this with:
    // data.map { "data.assertFile(\"${it.key}\", \"\"\"\n    |${it.value.replace("\$", "\${'$'}").replace("\n", "\n    |")}\n    \"\"\".trimMargin())" }.joinToString("\n")
    data.assertFile(
      "en.lproj/Localizable.strings",
      """
    |"key" = "Hello! I am great today! There you have some params %lld, %@, %e, %f";
    |
    |"key" = "String\nwith\nnewlines";
    |
    |
      """.trimMargin(),
    )
    data.assertFile(
      "en.lproj/Localizable.stringsdict",
      """
    |<?xml version="1.0" encoding="UTF-8"?>
    |<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
    |
    |<plist version="1.0">
    |  <dict>
    |    <key>key</key>
    |    <dict>
    |      <key>NSStringLocalizedFormatKey</key>
    |      <string>%#@format@</string>
    |      <key>format</key>
    |      <dict>
    |        <key>NSStringFormatSpecTypeKey</key>
    |        <string>NSStringPluralRuleType</string>
    |        <key>NSStringFormatValueTypeKey</key>
    |        <string>lld</string>
    |        <key>one</key>
    |        <string>%lld day</string>
    |        <key>other</key>
    |        <string>%lld days</string>
    |      </dict>
    |    </dict>
    |    <key>plural_numbered_placeholders</key>
    |    <dict>
    |      <key>NSStringLocalizedFormatKey</key>
    |      <string>%#@format@</string>
    |      <key>format</key>
    |      <dict>
    |        <key>NSStringFormatSpecTypeKey</key>
    |        <string>NSStringPluralRuleType</string>
    |        <key>NSStringFormatValueTypeKey</key>
    |        <string>lld</string>
    |        <key>one</key>
    |        <string>Today we had a very nice workout at %2${'$'}@, where we did one push up.</string>
    |        <key>other</key>
    |        <string>Today we had a very nice workout at %2${'$'}@, where we did %3${'$'}@ push ups.</string>
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
    |
      """.trimMargin(),
    )
    data.assertFile(
      "homepage/en.lproj/Localizable.stringsdict",
      """
    |<?xml version="1.0" encoding="UTF-8"?>
    |<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
    |
    |<plist version="1.0">
    |  <dict>
    |    <key>key</key>
    |    <dict>
    |      <key>NSStringLocalizedFormatKey</key>
    |      <string>%#@format@</string>
    |      <key>format</key>
    |      <dict>
    |        <key>NSStringFormatSpecTypeKey</key>
    |        <string>NSStringPluralRuleType</string>
    |        <key>NSStringFormatValueTypeKey</key>
    |        <string>lld</string>
    |        <key>one</key>
    |        <string>%lld day</string>
    |        <key>other</key>
    |        <string>%lld days</string>
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
    |
      """.trimMargin(),
    )
    data.assertFile(
      "cs.lproj/Localizable.strings",
      """
    |"escaping_singular" = "To je ale den \n \U0032";
    |
    |
      """.trimMargin(),
    )
    data.assertFile(
      "cs.lproj/Localizable.stringsdict",
      """
    |<?xml version="1.0" encoding="UTF-8"?>
    |<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
    |
    |<plist version="1.0">
    |  <dict>
    |    <key>key</key>
    |    <dict>
    |      <key>NSStringLocalizedFormatKey</key>
    |      <string>%#@format@</string>
    |      <key>format</key>
    |      <dict>
    |        <key>NSStringFormatSpecTypeKey</key>
    |        <string>NSStringPluralRuleType</string>
    |        <key>NSStringFormatValueTypeKey</key>
    |        <string>lld</string>
    |        <key>one</key>
    |        <string>%lld den</string>
    |        <key>few</key>
    |        <string>dny</string>
    |        <key>other</key>
    |        <string>%lld dnů</string>
    |      </dict>
    |    </dict>
    |    <key>escaping_plural</key>
    |    <dict>
    |      <key>NSStringLocalizedFormatKey</key>
    |      <string>%#@format@</string>
    |      <key>format</key>
    |      <dict>
    |        <key>NSStringFormatSpecTypeKey</key>
    |        <string>NSStringPluralRuleType</string>
    |        <key>NSStringFormatValueTypeKey</key>
    |        <string>lld</string>
    |        <key>one</key>
    |        <string>%lld den \n \U0032</string>
    |        <key>few</key>
    |        <string>dny</string>
    |        <key>other</key>
    |        <string>%lld dnů</string>
    |      </dict>
    |    </dict>
    |  </dict>
    |</plist>
    |
      """.trimMargin(),
    )
  }

  @Test
  fun `honors the provided fileStructureTemplate`() {
    val exporter =
      getExporter(
        params =
          getExportParams().also {
            it.fileStructureTemplate = "{languageTag}/hello/{namespace}.{extension}"
          },
      )
    val files = exporter.produceFiles()
    files["en/hello/homepage.strings"].assert.isNotNull()
    files["en/hello/homepage.stringsdict"].assert.isNotNull()
  }

  @Test
  fun `exports with placeholders (ICU placeholders enabled)`() {
    val exporter = getIcuPlaceholdersEnabledExporter()
    val data = getExported(exporter)
    data.assertFile(
      "cs.lproj/Localizable.strings",
      """
    |"item" = "I will be first {icuParam}";
    |
    |
      """.trimMargin(),
    )
    data.assertFile(
      "cs.lproj/Localizable.stringsdict",
      """
    |<?xml version="1.0" encoding="UTF-8"?>
    |<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
    |
    |<plist version="1.0">
    |  <dict>
    |    <key>key3</key>
    |    <dict>
    |      <key>NSStringLocalizedFormatKey</key>
    |      <string>%#@format@</string>
    |      <key>format</key>
    |      <dict>
    |        <key>NSStringFormatSpecTypeKey</key>
    |        <string>NSStringPluralRuleType</string>
    |        <key>NSStringFormatValueTypeKey</key>
    |        <string>lld</string>
    |        <key>one</key>
    |        <string>%lld den %@</string>
    |        <key>few</key>
    |        <string>%lld dny</string>
    |        <key>other</key>
    |        <string>%lld dní</string>
    |      </dict>
    |    </dict>
    |  </dict>
    |</plist>
    |
      """.trimMargin(),
    )
  }

  private fun getIcuPlaceholdersEnabledExporter(): AppleStringsStringsdictExporter {
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
      "cs.lproj/Localizable.strings",
      """
    |"item" = "I will be first {icuParam}";
    |
    |
      """.trimMargin(),
    )
    data.assertFile(
      "cs.lproj/Localizable.stringsdict",
      """
    |<?xml version="1.0" encoding="UTF-8"?>
    |<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
    |
    |<plist version="1.0">
    |  <dict>
    |    <key>key3</key>
    |    <dict>
    |      <key>NSStringLocalizedFormatKey</key>
    |      <string>%#@format@</string>
    |      <key>format</key>
    |      <dict>
    |        <key>NSStringFormatSpecTypeKey</key>
    |        <string>NSStringPluralRuleType</string>
    |        <key>NSStringFormatValueTypeKey</key>
    |        <string>lld</string>
    |        <key>one</key>
    |        <string># den {icuParam}</string>
    |        <key>few</key>
    |        <string># dny</string>
    |        <key>other</key>
    |        <string># dní</string>
    |      </dict>
    |    </dict>
    |  </dict>
    |</plist>
    |
      """.trimMargin(),
    )
  }

  private fun getIcuPlaceholdersDisabledExporter(): AppleStringsStringsdictExporter {
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

  private fun getExporter(params: ExportParams = getExportParams()) =
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
          ExportKeyView(1, "key", namespace = "homepage", isPlural = true),
          "en",
        ),
        ExportTranslationView(
          1,
          "String\nwith\nnewlines",
          TranslationState.TRANSLATED,
          ExportKeyView(1, "key"),
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
        ExportTranslationView(
          1,
          "{count, plural, one {# den \\n \\u0032} few {dny} other {# dnů}}",
          TranslationState.TRANSLATED,
          ExportKeyView(1, "escaping_plural", isPlural = true),
          "cs",
        ),
        ExportTranslationView(
          1,
          "To je ale den \\n \\u0032",
          TranslationState.TRANSLATED,
          ExportKeyView(1, "escaping_singular"),
          "cs",
        ),
        ExportTranslationView(
          1,
          "{value, plural, one {Today we had a very nice workout at {1}, where we did one push up.} other {Today we had a very nice workout at {1}, where we did {2} push ups.}}",
          TranslationState.TRANSLATED,
          ExportKeyView(1, "plural_numbered_placeholders", isPlural = true),
          "en",
        ),
      ),
      params = params,
    )

  private fun getExporter(
    translations: List<ExportTranslationView>,
    isProjectIcuPlaceholdersEnabled: Boolean = true,
    params: ExportParams = getExportParams(),
  ): AppleStringsStringsdictExporter {
    val template = ExportFileStructureTemplateProvider(params, translations).validateAndGetTemplate()
    return AppleStringsStringsdictExporter(
      translations = translations,
      exportParams = params,
      isProjectIcuPlaceholdersEnabled = isProjectIcuPlaceholdersEnabled,
      stringsFilePathProvider =
        ExportFilePathProvider(
          template = template,
          extension = "strings",
        ),
      stringsdictFilePathProvider =
        ExportFilePathProvider(
          template = template,
          extension = "stringsdict",
        ),
    )
  }

  private fun getExportParams() = ExportParams().also { it.format = ExportFormat.APPLE_STRINGS_STRINGSDICT }
}
