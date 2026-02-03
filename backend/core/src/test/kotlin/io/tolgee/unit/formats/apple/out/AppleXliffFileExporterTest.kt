package io.tolgee.unit.formats.apple.out

import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.apple.APPLE_FILE_ORIGINAL_CUSTOM_KEY
import io.tolgee.formats.apple.out.AppleXliffExporter
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.ExportFileStructureTemplateProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.testing.assert
import io.tolgee.util.buildExportTranslationList
import org.junit.jupiter.api.Test

class AppleXliffFileExporterTest {
  @Test
  fun exports() {
    val exporter = getExporter()

    val files = exporter.produceFiles()
    val data = files.map { it.key to it.value.bufferedReader().readText() }.toMap()

    // generate this with:
    // data.map { "data.assertFile(\"${it.key}\", \"\"\"\n    |${it.value.replace("\$", "\${'$'}").replace("\n", "\n    |")}\n    \"\"\".trimMargin())" }.joinToString("\n")
    data.assertFile(
      "cs.xliff",
      """
    |<?xml version="1.0" encoding="UTF-8" standalone="no"?>
    |<xliff xmlns="urn:oasis:names:tc:xliff:document:1.2" version="1.2">
    |  <file datatype="plaintext" original="Localizable.strings" source-language="tag" target-language="cs">
    |    <header>
    |      <tool tool-id="tolgee.io" tool-name="Tolgee"/>
    |    </header>
    |    <body>
    |      <trans-unit id="key1">
    |        <source xml:space="preserve">Hello! I%lld, %@, %e, %f</source>
    |        <target xml:space="preserve">Ahoj! I%lld, %@, %e, %f</target>
    |      </trans-unit>
    |      <trans-unit id="No base!">
    |        <source xml:space="preserve"/>
    |        <target xml:space="preserve">I have no base</target>
    |      </trans-unit>
    |      <trans-unit id="No target!">
    |        <source xml:space="preserve">I have no target</source>
    |      </trans-unit>
    |      <trans-unit id="escaping_singular">
    |        <source xml:space="preserve">{count, plural, one {# day} other {# days}}</source>
    |        <target xml:space="preserve">To je ale den \n \U0032</target>
    |      </trans-unit>
    |    </body>
    |  </file>
    |  <file datatype="plaintext" original="Localizable.stringsdict" source-language="tag" target-language="cs">
    |    <header>
    |      <tool tool-id="tolgee.io" tool-name="Tolgee"/>
    |    </header>
    |    <body>
    |      <trans-unit id="/key5:dict/NSStringLocalizedFormatKey:dict/:string">
    |        <source xml:space="preserve">%#@property@</source>
    |        <target xml:space="preserve">%#@property@</target>
    |      </trans-unit>
    |      <trans-unit id="/key5:dict/property:dict/one:dict/:string">
    |        <source xml:space="preserve">%lld day</source>
    |        <target xml:space="preserve">%lld den</target>
    |      </trans-unit>
    |      <trans-unit id="/key5:dict/property:dict/few:dict/:string">
    |        <source xml:space="preserve">%lld days</source>
    |        <target xml:space="preserve">%lld dnů</target>
    |      </trans-unit>
    |      <trans-unit id="/key5:dict/property:dict/many:dict/:string">
    |        <source xml:space="preserve">%lld days</source>
    |        <target xml:space="preserve">%lld dnů</target>
    |      </trans-unit>
    |      <trans-unit id="/key5:dict/property:dict/other:dict/:string">
    |        <source xml:space="preserve">%lld days</source>
    |        <target xml:space="preserve">%lld dnů</target>
    |      </trans-unit>
    |      <trans-unit id="/key6:dict/NSStringLocalizedFormatKey:dict/:string">
    |        <source xml:space="preserve">%#@property@</source>
    |        <target xml:space="preserve">%#@property@</target>
    |      </trans-unit>
    |      <trans-unit id="/key6:dict/property:dict/one:dict/:string">
    |        <source xml:space="preserve">%lld day</source>
    |        <target xml:space="preserve">%lld den</target>
    |      </trans-unit>
    |      <trans-unit id="/key6:dict/property:dict/few:dict/:string">
    |        <source xml:space="preserve">%lld days</source>
    |        <target xml:space="preserve">dny</target>
    |      </trans-unit>
    |      <trans-unit id="/key6:dict/property:dict/many:dict/:string">
    |        <source xml:space="preserve">%lld days</source>
    |        <target xml:space="preserve">%lld dnů</target>
    |      </trans-unit>
    |      <trans-unit id="/key6:dict/property:dict/other:dict/:string">
    |        <source xml:space="preserve">%lld days</source>
    |        <target xml:space="preserve">%lld dnů</target>
    |      </trans-unit>
    |      <trans-unit id="/escaping_plural:dict/NSStringLocalizedFormatKey:dict/:string">
    |        <source xml:space="preserve">%#@property@</source>
    |        <target xml:space="preserve">%#@property@</target>
    |      </trans-unit>
    |      <trans-unit id="/escaping_plural:dict/property:dict/one:dict/:string">
    |        <source xml:space="preserve">%lld day</source>
    |        <target xml:space="preserve">%lld den \n \U0032</target>
    |      </trans-unit>
    |      <trans-unit id="/escaping_plural:dict/property:dict/few:dict/:string">
    |        <source xml:space="preserve">%lld days</source>
    |        <target xml:space="preserve">dny</target>
    |      </trans-unit>
    |      <trans-unit id="/escaping_plural:dict/property:dict/many:dict/:string">
    |        <source xml:space="preserve">%lld days</source>
    |        <target xml:space="preserve">%lld dnů</target>
    |      </trans-unit>
    |      <trans-unit id="/escaping_plural:dict/property:dict/other:dict/:string">
    |        <source xml:space="preserve">%lld days</source>
    |        <target xml:space="preserve">%lld dnů</target>
    |      </trans-unit>
    |    </body>
    |  </file>
    |  <file datatype="plaintext" original="Localizable.xcstrings" source-language="tag" target-language="cs">
    |    <header>
    |      <tool tool-id="tolgee.io" tool-name="Tolgee"/>
    |    </header>
    |    <body>
    |      <trans-unit id="key &lt;omg&gt;|==|plural.one">
    |        <source xml:space="preserve">%lld day</source>
    |        <target xml:space="preserve">%lld day</target>
    |      </trans-unit>
    |      <trans-unit id="key &lt;omg&gt;|==|plural.few">
    |        <source xml:space="preserve">%lld days</source>
    |        <target xml:space="preserve">%lld days</target>
    |      </trans-unit>
    |      <trans-unit id="key &lt;omg&gt;|==|plural.many">
    |        <source xml:space="preserve">%lld days</source>
    |        <target xml:space="preserve">%lld days</target>
    |      </trans-unit>
    |      <trans-unit id="key &lt;omg&gt;|==|plural.other">
    |        <source xml:space="preserve">%lld days</source>
    |        <target xml:space="preserve">%lld days</target>
    |      </trans-unit>
    |    </body>
    |  </file>
    |</xliff>
    |
      """.trimMargin(),
    )
    data.assertFile(
      "homepage/cs.xliff",
      """
    |<?xml version="1.0" encoding="UTF-8" standalone="no"?>
    |<xliff xmlns="urn:oasis:names:tc:xliff:document:1.2" version="1.2">
    |  <file datatype="plaintext" original="homepage.strings" source-language="tag" target-language="cs">
    |    <header>
    |      <tool tool-id="tolgee.io" tool-name="Tolgee"/>
    |    </header>
    |    <body>
    |      <trans-unit id="key2">
    |        <source xml:space="preserve">Namespaced</source>
    |        <target xml:space="preserve">Namespaced</target>
    |      </trans-unit>
    |    </body>
    |  </file>
    |  <file datatype="plaintext" original="homepage.stringsdict" source-language="tag" target-language="cs">
    |    <header>
    |      <tool tool-id="tolgee.io" tool-name="Tolgee"/>
    |    </header>
    |    <body>
    |      <trans-unit id="/key3:dict/NSStringLocalizedFormatKey:dict/:string">
    |        <source xml:space="preserve">%#@property@</source>
    |        <target xml:space="preserve">%#@property@</target>
    |      </trans-unit>
    |      <trans-unit id="/key3:dict/property:dict/one:dict/:string">
    |        <source xml:space="preserve">%lld day</source>
    |        <target xml:space="preserve">%lld den</target>
    |      </trans-unit>
    |      <trans-unit id="/key3:dict/property:dict/few:dict/:string">
    |        <source xml:space="preserve">%lld days</source>
    |        <target xml:space="preserve">%lld dny</target>
    |      </trans-unit>
    |      <trans-unit id="/key3:dict/property:dict/many:dict/:string">
    |        <source xml:space="preserve">%lld days</source>
    |        <target xml:space="preserve">%lld dní</target>
    |      </trans-unit>
    |      <trans-unit id="/key3:dict/property:dict/other:dict/:string">
    |        <source xml:space="preserve">%lld days</source>
    |        <target xml:space="preserve">%lld dní</target>
    |      </trans-unit>
    |    </body>
    |  </file>
    |  <file datatype="plaintext" original="Localizable.strings" source-language="tag" target-language="cs">
    |    <header>
    |      <tool tool-id="tolgee.io" tool-name="Tolgee"/>
    |    </header>
    |    <body>
    |      <trans-unit id="key4">
    |        <source xml:space="preserve">Namespaced</source>
    |        <target xml:space="preserve">Namespaced</target>
    |      </trans-unit>
    |    </body>
    |  </file>
    |</xliff>
    |
      """.trimMargin(),
    )
  }

  @Test
  fun `exports with placeholders (ICU placeholders enabled)`() {
    val exporter = getIcuPlaceholdersEnabledExporter()
    val data = getExported(exporter)
    data.assertFile(
      "cs.xliff",
      """
    |<?xml version="1.0" encoding="UTF-8" standalone="no"?>
    |<xliff xmlns="urn:oasis:names:tc:xliff:document:1.2" version="1.2">
    |  <file datatype="plaintext" original="Localizable.stringsdict" source-language="tag" target-language="cs">
    |    <header>
    |      <tool tool-id="tolgee.io" tool-name="Tolgee"/>
    |    </header>
    |    <body>
    |      <trans-unit id="/key3:dict/NSStringLocalizedFormatKey:dict/:string">
    |        <source xml:space="preserve"/>
    |        <target xml:space="preserve">%#@property@</target>
    |      </trans-unit>
    |      <trans-unit id="/key3:dict/property:dict/one:dict/:string">
    |        <source xml:space="preserve"/>
    |        <target xml:space="preserve">%lld den %@</target>
    |      </trans-unit>
    |      <trans-unit id="/key3:dict/property:dict/few:dict/:string">
    |        <source xml:space="preserve"/>
    |        <target xml:space="preserve">%lld dny</target>
    |      </trans-unit>
    |      <trans-unit id="/key3:dict/property:dict/many:dict/:string">
    |        <source xml:space="preserve"/>
    |        <target xml:space="preserve">%lld dní</target>
    |      </trans-unit>
    |      <trans-unit id="/key3:dict/property:dict/other:dict/:string">
    |        <source xml:space="preserve"/>
    |        <target xml:space="preserve">%lld dní</target>
    |      </trans-unit>
    |    </body>
    |  </file>
    |  <file datatype="plaintext" original="Localizable.strings" source-language="tag" target-language="cs">
    |    <header>
    |      <tool tool-id="tolgee.io" tool-name="Tolgee"/>
    |    </header>
    |    <body>
    |      <trans-unit id="item">
    |        <source xml:space="preserve"/>
    |        <target xml:space="preserve">I will be first {icuParam}</target>
    |      </trans-unit>
    |    </body>
    |  </file>
    |</xliff>
    |
      """.trimMargin(),
    )
  }

  @Test
  fun `exports with placeholders (ICU placeholders disabled)`() {
    val exporter = getIcuPlaceholdersDisabledExporter()
    val data = getExported(exporter)
    data.assertFile(
      "cs.xliff",
      """
    |<?xml version="1.0" encoding="UTF-8" standalone="no"?>
    |<xliff xmlns="urn:oasis:names:tc:xliff:document:1.2" version="1.2">
    |  <file datatype="plaintext" original="Localizable.stringsdict" source-language="tag" target-language="cs">
    |    <header>
    |      <tool tool-id="tolgee.io" tool-name="Tolgee"/>
    |    </header>
    |    <body>
    |      <trans-unit id="/key3:dict/NSStringLocalizedFormatKey:dict/:string">
    |        <source xml:space="preserve"/>
    |        <target xml:space="preserve">%#@property@</target>
    |      </trans-unit>
    |      <trans-unit id="/key3:dict/property:dict/one:dict/:string">
    |        <source xml:space="preserve"/>
    |        <target xml:space="preserve"># den {icuParam}</target>
    |      </trans-unit>
    |      <trans-unit id="/key3:dict/property:dict/few:dict/:string">
    |        <source xml:space="preserve"/>
    |        <target xml:space="preserve"># dny</target>
    |      </trans-unit>
    |      <trans-unit id="/key3:dict/property:dict/many:dict/:string">
    |        <source xml:space="preserve"/>
    |        <target xml:space="preserve"># dní</target>
    |      </trans-unit>
    |      <trans-unit id="/key3:dict/property:dict/other:dict/:string">
    |        <source xml:space="preserve"/>
    |        <target xml:space="preserve"># dní</target>
    |      </trans-unit>
    |    </body>
    |  </file>
    |  <file datatype="plaintext" original="Localizable.strings" source-language="tag" target-language="cs">
    |    <header>
    |      <tool tool-id="tolgee.io" tool-name="Tolgee"/>
    |    </header>
    |    <body>
    |      <trans-unit id="item">
    |        <source xml:space="preserve"/>
    |        <target xml:space="preserve">I will be first {icuParam}</target>
    |      </trans-unit>
    |    </body>
    |  </file>
    |</xliff>
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
    files["cs/hello/homepage.xliff"].assert.isNotNull()
  }

  private fun getExported(exporter: AppleXliffExporter): Map<String, String> {
    val files = exporter.produceFiles()
    val data = files.map { it.key to it.value.bufferedReader().readText() }.toMap()
    return data
  }

  private fun Map<String, String>.assertFile(
    file: String,
    content: String,
  ) {
    this[file]!!.assert.isEqualToNormalizingNewlines(content)
  }

  private fun getExporter(params: ExportParams = getExportParams()): AppleXliffExporter {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "key1",
          text =
            "Ahoj! I" +
              "{number, number}, {name}, {number, number, scientific}, " +
              "{number, number, 0.000000}",
          baseText =
            "Hello! I" +
              "{number, number}, {name}, {number, number, scientific}, " +
              "{number, number, 0.000000}",
        )
        add(
          languageTag = "cs",
          keyName = "No base!",
          text = "I have no base",
        )
        add(
          languageTag = "cs",
          keyName = "No target!",
          text = null,
          baseText = "I have no target",
        )
        add(
          languageTag = "cs",
          keyName = "key2",
          text = "Namespaced",
          baseText = "Namespaced",
        ) {
          key.namespace = "homepage"
        }
        add(
          languageTag = "cs",
          keyName = "key3",
          text = "{count, plural, one {# den} few {# dny} other {# dní}}",
          baseText = "{count, plural, one {# day} other {# days}}",
        ) {
          key.namespace = "homepage"
          key.description = "This is a description\n With some  spaces \n\n to preserve."
          key.isPlural = true
        }
        add(
          languageTag = "cs",
          keyName = "key4",
          text = "Namespaced",
          baseText = "Namespaced",
        ) {
          key.namespace = "homepage"
          key.custom = mapOf(APPLE_FILE_ORIGINAL_CUSTOM_KEY to "Localizable.strings")
        }
        add(
          languageTag = "cs",
          keyName = "key5",
          text = "{count, plural, one {# den} other {# dnů}}",
          baseText = "{count, plural, one {# day} other {# days}}",
        ) {
          key.isPlural = true
        }
        add(
          languageTag = "cs",
          keyName = "key <omg>",
          text = "{count, plural, one {# day} other {# days}}",
          baseText = "{count, plural, one {# day} other {# days}}",
        ) {
          key.isPlural = true
          key.custom = mapOf(APPLE_FILE_ORIGINAL_CUSTOM_KEY to "Localizable.xcstrings")
        }
        add(
          languageTag = "cs",
          keyName = "key6",
          text = "{count, plural, one {# den} few {dny} other {# dnů}}",
          baseText = "{count, plural, one {# day} other {# days}}",
        ) {
          key.isPlural = true
          key.custom = mapOf(APPLE_FILE_ORIGINAL_CUSTOM_KEY to "Localizable.stringsdict")
        }
        add(
          languageTag = "cs",
          keyName = "escaping_plural",
          text = "{count, plural, one {# den \\n \\u0032} few {dny} other {# dnů}}",
          baseText = "{count, plural, one {# day} other {# days}}",
        ) {
          key.isPlural = true
          key.custom = mapOf(APPLE_FILE_ORIGINAL_CUSTOM_KEY to "Localizable.stringsdict")
        }
        add(
          languageTag = "cs",
          keyName = "escaping_singular",
          text = "To je ale den \\n \\u0032",
          baseText = "{count, plural, one {# day} other {# days}}",
        ) {
          key.custom = mapOf(APPLE_FILE_ORIGINAL_CUSTOM_KEY to "Localizable.strings")
        }
      }
    return getExporter(built.translations, built.baseTranslations, params = params)
  }

  private fun getIcuPlaceholdersEnabledExporter(): AppleXliffExporter {
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
    return getExporter(built.translations, emptyList(), true)
  }

  private fun getIcuPlaceholdersDisabledExporter(): AppleXliffExporter {
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
    return getExporter(built.translations, emptyList(), false)
  }

  private fun getExporter(
    translations: List<ExportTranslationView>,
    baseTranslations: List<ExportTranslationView>,
    isProjectIcuPlaceholdersEnabled: Boolean = true,
    params: ExportParams = getExportParams(),
  ): AppleXliffExporter {
    return AppleXliffExporter(
      translations = translations,
      baseTranslationsProvider = { baseTranslations },
      baseLanguageTag = "tag",
      isProjectIcuPlaceholdersEnabled = isProjectIcuPlaceholdersEnabled,
      filePathProvider =
        ExportFilePathProvider(
          template = ExportFileStructureTemplateProvider(params, translations).validateAndGetTemplate(),
          extension = params.format.extension,
        ),
    )
  }

  private fun getExportParams(): ExportParams {
    return ExportParams().also { it.format = ExportFormat.APPLE_XLIFF }
  }
}
