package io.tolgee.formats.genericStructuredFile.`in`

import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.MessageConvertorResult
import io.tolgee.formats.allPluralKeywords
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.formats.importCommon.ParsedPluralsKey
import io.tolgee.formats.importCommon.PluralsKeyParser
import io.tolgee.service.dataImport.processors.FileProcessorContext

class GenericStructuredProcessor(
  override val context: FileProcessorContext,
  private val data: Any?,
  private val convertor: StructuredRawDataConvertor,
  private val languageTag: String? = null,
  private val format: ImportFormat,
) : ImportFileProcessor() {
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

  private fun Any?.parsePluralsKey(keyParser: PluralsKeyParser): ParsedPluralsKey? {
    val key = this as? String ?: return null
    return keyParser.parse(key).takeIf {
      it.key != null && it.plural in allPluralKeywords
    } ?: ParsedPluralsKey(null, null, key)
  }

  private fun Map<*, *>.groupByPlurals(keyParser: PluralsKeyParser): Map<String?, List<Pair<ParsedPluralsKey, Any?>>> {
    return this.entries.mapIndexedNotNull { idx, (key, value) ->
      key.parsePluralsKey(keyParser)?.let { it to value }.also {
        if (it == null) {
          context.fileEntity.addKeyIsNotStringIssue(key.toString(), idx)
        }
      }
    }.groupBy { (parsedKey, _) -> parsedKey.key }.toMap()
  }

  private fun List<Pair<ParsedPluralsKey, Any?>>.useOriginalKey(): List<Pair<String, Any?>> {
    return map { (parsedKey, value) ->
      parsedKey.originalKey to value
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
    if (format.pluralsViaSuffixesParser == null) {
      return this.mapValues { (_, value) -> value.preprocess() }
    }

    val plurals = this.groupByPlurals(format.pluralsViaSuffixesParser)

    return plurals.flatMap { (commonKey, values) ->
      if (commonKey == null || values.size < 2) {
        values.useOriginalKey()
      } else {
        values.usePluralsKey(commonKey)
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
