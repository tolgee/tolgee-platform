package io.tolgee.formats.genericStructuredFile.`in`

import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.MessageConvertorResult
import io.tolgee.formats.allPluralKeywords
import io.tolgee.formats.i18next.ParsedI18nextKey
import io.tolgee.formats.i18next.PluralsI18nextKeyParser
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.service.dataImport.processors.FileProcessorContext

class GenericStructuredProcessor(
  override val context: FileProcessorContext,
  private val data: Any?,
  private val convertor: StructuredRawDataConvertor,
  private val languageTag: String? = null,
  private val format: ImportFormat,
) : ImportFileProcessor() {
  private val keyParser = PluralsI18nextKeyParser()

  override fun process() {
    data.preprocess().import("")
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

  private fun Map<*, *>.groupByPlurals(keyRegex: Regex): Map<String?, List<Pair<ParsedI18nextKey, Any?>>> {
    return this.entries.mapIndexedNotNull { idx, (key, value) ->
      if (key !is String) {
        context.fileEntity.addKeyIsNotStringIssue(key.toString(), idx)
        return@mapIndexedNotNull null
      }
      val default = ParsedI18nextKey(null, null, key)

      val match = keyRegex.find(key) ?: return@mapIndexedNotNull default to value
      val parsedKey = keyParser.parse(match)

      if (parsedKey?.key == null || parsedKey.plural == null || parsedKey.plural !in allPluralKeywords) {
        return@mapIndexedNotNull default to value
      }

      return@mapIndexedNotNull parsedKey to value
    }.groupBy { (parsedKey, _) ->
      parsedKey.key
    }.toMap()
  }

  private fun Map<*, *>.preprocessMap(): Map<*, *> {
    if (format.pluralsViaSuffixesRegex == null) {
      return this.mapValues { (_, value) -> value.preprocess() }
    }

    val plurals = this.groupByPlurals(format.pluralsViaSuffixesRegex)

    return plurals.flatMap { (key, values) ->
      if (key == null || values.size < 2) {
        // Fallback for non-plural keys
        values.map { (parsedKey, value) ->
          parsedKey.fullMatch to value
        }
      } else {
        listOf(key to values.map { (parsedKey, value) ->
          parsedKey.plural to value
        }.toMap())
      }
    }.toMap()
  }

  private fun Any?.import(key: String) {
    // Convertor handles strings and possible nested plurals, if convertor returns null,
    // it means that it's not a string or nested plurals, so we need to parse it further
    convert(this)?.let { result ->
      result.forEach {
        context.addTranslation(
          key,
          languageTagOrGuess,
          it.message,
          rawData = this@import,
          convertedBy = format,
          pluralArgName = it.pluralArgName,
        )
      }
      return
    }

    (this as? List<*>)?.let {
      it.parseList(key)
      return
    }

    (this as? Map<*, *>)?.let {
      it.parseMap(key)
      return
    }

    convert(this)?.firstOrNull()?.let {
      context.addTranslation(
        keyName = key,
        languageName = languageTagOrGuess,
        value = it.message,
        pluralArgName = it.pluralArgName,
        rawData = this@import,
        convertedBy = format,
      )
    }
  }

  private fun convert(data: Any?): List<MessageConvertorResult>? {
    return convertor.convert(
      rawData = data,
      projectIcuPlaceholdersEnabled = context.projectIcuPlaceholdersEnabled,
      convertPlaceholdersToIcu = context.importSettings.convertPlaceholdersToIcu,
    )
  }

  private fun List<*>.parseList(keyPrefix: String) {
    this.forEachIndexed { idx, it ->
      it.import("$keyPrefix[$idx]")
    }
  }

  private fun Map<*, *>.parseMap(keyPrefix: String) {
    this.entries.forEachIndexed { idx, entry ->
      val key = entry.key

      if (key !is String) {
        context.fileEntity.addKeyIsNotStringIssue(key.toString(), idx)
        return@forEachIndexed
      }

      val keyPrefixWithDelimiter =
        if (keyPrefix.isNotEmpty()) "$keyPrefix${context.params.structureDelimiter}" else ""

      entry.value.import("$keyPrefixWithDelimiter$key")
      return@forEachIndexed
    }
  }

  private val languageTagOrGuess: String by lazy {
    languageTag ?: firstLanguageTagGuessOrUnknown
  }
}
