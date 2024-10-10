package io.tolgee.formats.android.out

import io.tolgee.dtos.IExportParams
import io.tolgee.formats.PossiblePluralConversionResult
import io.tolgee.formats.android.ANDROID_CDATA_CUSTOM_KEY
import io.tolgee.formats.android.AndroidStringValue
import io.tolgee.formats.android.AndroidStringsXmlModel
import io.tolgee.formats.android.AndroidXmlNode
import io.tolgee.formats.android.PluralUnit
import io.tolgee.formats.android.StringArrayItem
import io.tolgee.formats.android.StringArrayUnit
import io.tolgee.formats.android.StringUnit
import io.tolgee.formats.populateForms
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import java.io.InputStream

class AndroidStringsXmlExporter(
  val translations: List<ExportTranslationView>,
  val exportParams: IExportParams,
  private val isProjectIcuPlaceholdersEnabled: Boolean = true,
) : FileExporter {
  /**
   * Map (Path To file -> Map (Key Name -> Node Wrapper))
   */
  private val fileUnits = mutableMapOf<String, MutableMap<String, NodeWrapper>>()

  private fun getModels(): Map<String, AndroidStringsXmlModel> {
    prepare()

    return fileUnits.map { (pathToFile, units) ->
      val model = AndroidStringsXmlModel()

      units.forEach {
        model.items[it.key] = it.value.node
      }

      pathToFile to model
    }.toMap()
  }

  private fun prepare() {
    translations.forEach { translation ->
      val arrayMatch = KEY_IS_ARRAY_REGEX.matchEntire(translation.key.name)
      val isArray = arrayMatch != null
      when {
        isArray -> buildStringArrayUnit(translation, arrayMatch)
        else -> {
          val converted = getConvertedMessage(translation, translation.key.isPlural)
          when {
            converted.isPlural() -> buildPluralsUnit(translation, converted.formsResult!!)
            else -> buildStringUnit(translation, converted.singleResult!!)
          }
        }
      }
    }
  }

  private fun buildStringUnit(
    translation: ExportTranslationView,
    text: String,
  ) {
    val stringUnit =
      StringUnit().apply {
        this.value = AndroidStringValue(text, translation.isWrappedWithCdata())
        this.comment = translation.description
      }
    addToUnits(translation, stringUnit)
  }

  private fun ExportTranslationView.isWrappedWithCdata(): Boolean {
    return this.key.custom?.get(ANDROID_CDATA_CUSTOM_KEY) == true
  }

  private fun buildStringArrayUnit(
    translation: ExportTranslationView,
    arrayMatch: MatchResult,
  ) {
    // Assuming your translation view contains an array of strings as value for string arrays
    val index = arrayMatch.groups["index"]?.value?.toIntOrNull() ?: return
    val keyNameWithoutIndex = arrayMatch.groups["name"]?.value ?: return
    addStringArrayUnitItem(keyNameWithoutIndex, index, translation)
  }

  private fun addStringArrayUnitItem(
    keyNameWithoutIndex: String,
    index: Int,
    translation: ExportTranslationView,
  ) {
    val normalizedKeyName = keyNameWithoutIndex.normalizedKeyName()
    val isExactKeyName = normalizedKeyName == keyNameWithoutIndex
    val units = getFileUnits(translation)
    val text = getConvertedMessage(translation, false).singleResult!!
    units.compute(normalizedKeyName) { _, stringsArrayWrapper ->
      when {
        // it does not exist yer, or was created from key which have been normalized to different value
        // in that case key without normalizing takes precedence
        stringsArrayWrapper == null || (!stringsArrayWrapper.isExactKeyName && isExactKeyName) -> {
          NodeWrapper(
            StringArrayUnit().apply {
              this.items.add(
                StringArrayItem(
                  AndroidStringValue(text, translation.isWrappedWithCdata()),
                  index,
                  comment = translation.description,
                ),
              )
            },
            isExactKeyName,
            keyNameWithoutIndex,
          )
        }

        // it is already a string array and the key is the same
        (stringsArrayWrapper.node is StringArrayUnit && keyNameWithoutIndex == stringsArrayWrapper.exactKeyName) -> {
          stringsArrayWrapper.node.items.add(
            StringArrayItem(
              AndroidStringValue(text, translation.isWrappedWithCdata()),
              index,
              comment = translation.description,
            ),
          )
          stringsArrayWrapper
        }

        else -> {
          stringsArrayWrapper
        }
      }
    }
  }

  private fun buildPluralsUnit(
    translation: ExportTranslationView,
    pluralForms: Map<String, String>,
  ) {
    // Assuming your translation view contain a map of plural forms as value
    val pluralMap =
      populateForms(translation.languageTag, pluralForms).map {
        it.key to AndroidStringValue(it.value, translation.isWrappedWithCdata())
      }.toMap()

    val pluralUnit =
      PluralUnit().apply {
        this.items.putAll(pluralMap)
        this.comment = translation.description
      }

    addToUnits(translation, pluralUnit)
  }

  private fun addToUnits(
    translation: ExportTranslationView,
    node: AndroidXmlNode,
  ) {
    val keyName = translation.key.name
    val normalizedName = keyName.normalizedKeyName()
    val isExactKeyName = normalizedName == keyName
    val units = getFileUnits(translation)
    val existingUnit = units[normalizedName]

    // key with exact name was added before
    if (existingUnit != null && existingUnit.isExactKeyName) {
      return
    }

    units[normalizedName] = NodeWrapper(node, isExactKeyName, keyName)
  }

  private fun String.normalizedKeyName() = replace(KEY_REPLACE_REGEX, "_").replace("__", "_")

  private class NodeWrapper(
    val node: AndroidXmlNode,
    // is the keyName same as before normalization
    val isExactKeyName: Boolean,
    // the key name before normalization
    val exactKeyName: String,
  )

  private fun getFileUnits(translation: ExportTranslationView): MutableMap<String, NodeWrapper> {
    val filePath =
      pathProvider.getFilePath(languageTag = translation.languageTag, namespace = translation.key.namespace)
    return fileUnits.computeIfAbsent(filePath) { mutableMapOf() }
  }

  private val pathProvider by lazy {
    ExportFilePathProvider(
      exportParams,
      "xml",
    )
  }

  private fun getConvertedMessage(
    translation: ExportTranslationView,
    isPlural: Boolean = translation.key.isPlural,
  ): PossiblePluralConversionResult {
    val converted =
      IcuToJavaMessageConvertor(
        translation.text ?: "",
        isPlural,
        isProjectIcuPlaceholdersEnabled,
      ).convert()

    return converted
  }

  override fun produceFiles(): Map<String, InputStream> {
    return getModels().map { (path, model) ->
      path to AndroidStringsXmlFileWriter(model).produceFiles()
    }.toMap()
  }

  companion object {
    val KEY_IS_ARRAY_REGEX by lazy {
      Regex("(?<name>.*)\\[(?<index>\\d+)\\]$")
    }
    val KEY_REPLACE_REGEX by lazy {
      Regex("[^a-zA-Z0-9_]")
    }
  }
}
