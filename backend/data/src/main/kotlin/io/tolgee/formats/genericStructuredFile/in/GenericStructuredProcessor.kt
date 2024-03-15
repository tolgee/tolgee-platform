package io.tolgee.formats.genericStructuredFile.`in`

import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.service.dataImport.processors.FileProcessorContext

class GenericStructuredProcessor(
  override val context: FileProcessorContext,
  private val data: Any?,
  private val convertor: StructuredRawDataConvertor,
  private val languageTag: String? = null,
  private val initialKeyPath: List<Any> = listOf(),
) : ImportFileProcessor() {
  override fun process() {
    try {
      data.import("", initialKeyPath)
    } catch (e: Exception) {
      throw ImportCannotParseFileException(context.file.name, e.message ?: "")
    }
  }

  private fun Any?.import(
    key: String,
    keyPath: List<Any>,
  ) {
    // Convertor handles strings and possible nested plurals, if convertor returns null,
    // it means that it's not a string or nested plurals, so we need to parse it further
    convert(this)?.let { result ->
      result.forEach {
        context.addKeyPath(key, keyPath)
        context.addTranslation(key, languageTagOrGuess, it.value, rawData = this, forceIsPlural = it.isPlural)
      }
      return
    }

    (this as? List<*>)?.let {
      it.parseList(key, keyPath)
      return
    }

    (this as? Map<*, *>)?.let {
      it.parseMap(key, keyPath)
      return
    }

    convert(this)?.firstOrNull()?.let {
      context.addKeyPath(key, keyPath)
      context.addTranslation(key, languageTagOrGuess, it.value, forceIsPlural = it.isPlural)
    }
  }

  private fun convert(data: Any?): List<StructuredRawDataConversionResult>? {
    return convertor.convert(
      rawData = data,
      projectIcuPlaceholdersEnabled = context.projectIcuPlaceholdersEnabled,
      convertPlaceholdersToIcu = context.importSettings.convertPlaceholdersToIcu,
    )
  }

  private fun List<*>.parseList(
    keyPrefix: String,
    keyPath: List<Any>,
  ) {
    this.forEachIndexed { idx, it ->
      it.import("$keyPrefix[$idx]", keyPath + idx)
    }
  }

  private fun Map<*, *>.parseMap(
    keyPrefix: String,
    keyPath: List<Any>,
  ) {
    this.entries.forEachIndexed { idx, entry ->
      val key = entry.key

      if (key !is String) {
        context.fileEntity.addKeyIsNotStringIssue(key.toString(), idx)
        return@forEachIndexed
      }

      val keyPrefixWithDelimiter =
        if (keyPrefix.isNotEmpty()) "$keyPrefix${context.params.structureDelimiter}" else ""

      entry.value.import("$keyPrefixWithDelimiter$key", keyPath + key)
      return@forEachIndexed
    }
  }

  private val languageTagOrGuess: String by lazy {
    languageTag ?: languageNameGuesses.firstOrNull() ?: "unknown"
  }
}
