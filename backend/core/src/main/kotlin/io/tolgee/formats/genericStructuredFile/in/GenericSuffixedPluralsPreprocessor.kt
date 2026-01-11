package io.tolgee.formats.genericStructuredFile.`in`

import io.tolgee.formats.allPluralKeywords
import io.tolgee.formats.getPluralFormsForLocaleOrAll
import io.tolgee.formats.importCommon.ParsedPluralsKey
import io.tolgee.formats.importCommon.PluralsKeyParser
import io.tolgee.service.dataImport.processors.FileProcessorContext

class GenericSuffixedPluralsPreprocessor(
  val context: FileProcessorContext,
  private val data: Any?,
  private val pluralsViaSuffixesParser: PluralsKeyParser,
  private val languageTag: String,
) {
  fun preprocess(): Any? {
    return data.preprocess()
  }

  private fun Any?.preprocess(): Any? {
    if (this == null) {
      return null
    }

    (this as? List<*>)?.let {
      return it.preprocessList()
    }

    (this as? Map<*, *>)?.let {
      return it.preprocessMap()
    }

    return this
  }

  private fun List<*>.preprocessList(): List<*> {
    return this.map { it.preprocess() }
  }

  private fun Any?.parsePluralsKey(keyParser: PluralsKeyParser): ParsedPluralsKey? {
    val key = this as? String ?: return null
    return keyParser.parse(key).takeIf {
      it.key != null && it.plural in allPluralKeywords
    } ?: ParsedPluralsKey(null, null, key)
  }

  private fun Map<*, *>.groupByPlurals(keyParser: PluralsKeyParser): Map<String?, List<Pair<ParsedPluralsKey, Any?>>> {
    return this.entries
      .mapIndexedNotNull { idx, (key, value) ->
        key.parsePluralsKey(keyParser)?.let { it to value }.also {
          if (it == null) {
            context.fileEntity.addKeyIsNotStringIssue(key.toString(), idx)
          }
        }
      }.groupBy { (parsedKey, _) -> parsedKey.key }
      .toMap()
  }

  private fun List<Pair<ParsedPluralsKey, Any?>>.useOriginalKey(): List<Pair<String, Any?>> {
    return map { (parsedKey, value) ->
      parsedKey.originalKey to value.preprocess()
    }
  }

  private fun List<Pair<ParsedPluralsKey, Any?>>.usePluralsKey(commonKey: String): List<Pair<String, Any?>> {
    return listOf(
      commonKey to
        this.associate { (parsedKey, value) ->
          parsedKey.plural to value
        },
    )
  }

  private fun Map<*, *>.preprocessMap(): Map<*, *> {
    return this
      .groupByPlurals(pluralsViaSuffixesParser)
      .flatMap { (commonKey, values) ->
        if (commonKey == null || (values.size < pluralKeywords.size && values.size < 2)) {
          return@flatMap values.useOriginalKey()
        }
        return@flatMap values.usePluralsKey(commonKey)
      }.toMap()
  }

  private val pluralKeywords by lazy {
    getPluralFormsForLocaleOrAll(languageTag)
  }
}
