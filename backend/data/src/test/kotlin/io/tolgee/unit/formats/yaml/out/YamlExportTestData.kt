package io.tolgee.unit.formats.yaml.out

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.genericStructuredFile.out.CustomPrettyPrinter
import io.tolgee.formats.yaml.out.YamlFileExporter
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.util.buildExportTranslationList

object YamlExportTestData {
  fun getIcuPlaceholdersDisabledExporter(): YamlFileExporter {
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
          text = "I will be first {icuParam, number}",
        )
      }
    return getExporter(built.translations, false)
  }

  fun getAllFeaturesExporter(exportParams: ExportParams? = null): YamlFileExporter {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "this.is.nested.plural",
          text = "{count, plural, one {# den {icuParam}} few {# dny} other {# dní}}",
        ) {
          key.isPlural = true
        }
        add(
          languageTag = "cs",
          keyName = "this[1].is.collision",
          text = "Colission",
        )
        add(
          languageTag = "cs",
          keyName = "this-is-array[1].object",
          text = "I will be first {icuParam, number}",
        )
        add(
          languageTag = "cs",
          keyName = "indexed params",
          text = "I will be first {0}, {1}",
        )
        add(
          languageTag = "cs",
          keyName = "named params",
          text = "I will be first {param1}, {param2}",
        )
      }
    return getExporter(built.translations, true, exportParams)
  }

  fun getIcuPlaceholdersEnabledExporter(): YamlFileExporter {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "key3",
          text = "{count, plural, one {# den {icuParam, number}} few {# dny} other {# dní}}",
        ) {
          key.isPlural = true
        }
        add(
          languageTag = "cs",
          keyName = "item",
          text = "I will be first '{'icuParam'}' {hello, number}",
        )
      }
    return getExporter(built.translations, true)
  }

  fun getExporter(
    translations: List<ExportTranslationView>,
    isProjectIcuPlaceholdersEnabled: Boolean = true,
    exportParams: ExportParams? = null,
  ): YamlFileExporter {
    return YamlFileExporter(
      translations = translations,
      exportParams =
        exportParams ?: ExportParams().also {
          it.supportArrays = true
          it.structureDelimiter = '.'
          it.format = ExportFormat.YAML_RUBY
        },
      projectIcuPlaceholdersSupport = isProjectIcuPlaceholdersEnabled,
      objectMapper = ObjectMapper(YAMLFactory()),
      customPrettyPrinter = CustomPrettyPrinter(),
    )
  }
}
