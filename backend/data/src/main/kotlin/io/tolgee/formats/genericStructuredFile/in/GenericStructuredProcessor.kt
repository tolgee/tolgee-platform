package io.tolgee.formats.genericStructuredFile.`in`

import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.service.dataImport.processors.FileProcessorContext

class GenericStructuredProcessor(
  override val context: FileProcessorContext,
  private val data: Any?,
  private val convertor: StructuredRawDataConvertor,
  private val languageTag: String? = null,
) : ImportFileProcessor() {
//  val result = mutableMapOf<String, MutableList<String?>>()
  override fun process() {
    try {
      data.import("")
//      result.entries.forEachIndexed { index, (key, translationTexts) ->
//        translationTexts.forEach { text ->
//          context.addGenericFormatTranslation(key, languageTagOrGuess, text, index)
//        }
//      }
    } catch (e: Exception) {
      throw ImportCannotParseFileException(context.file.name, e.message ?: "")
    }
  }

  private fun Any?.import(keyPrefix: String) {
    // Convertor handles strings and possible nested plurals, if convertor returns null,
    // it means that it's not a string or nested plurals, so we need to parse it further
    convert(keyPrefix, this)?.let { result ->
      result.forEach {
        context.addTranslation(it.key, languageTagOrGuess, it.value, forceIsPlural = it.isPlural)
      }
      return
    }

    (this as? List<*>)?.let {
      it.parseList(keyPrefix)
      return
    }

    (this as? Map<*, *>)?.let {
      it.parseMap(keyPrefix)
      return
    }

    convert(keyPrefix, this)?.firstOrNull()?.let {
      context.addTranslation(it.key, languageTagOrGuess, it.value, forceIsPlural = it.isPlural)
    }
  }

  private fun convert(
    keyPrefix: String,
    data: Any?,
  ): List<StructuredRawDataConversionResult>? {
    return convertor.convert(
      keyPrefix = keyPrefix,
      rawData = data,
      projectIcuPlaceholdersEnabled = context.projectIcuPlaceholdersEnabled,
      convertPlaceholdersToIcu = context.importSettings.convertPlaceholdersToIcu,
    )
  }

//  private fun addToResult(
//    key: String,
//    value: String?,
//  ) {
//    result.compute(key) { _, v ->
//      val list = v ?: mutableListOf()
//      list.add(value)
//      list
//    }
//  }

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
    languageTag ?: languageNameGuesses.firstOrNull() ?: "unknown"
  }
}
