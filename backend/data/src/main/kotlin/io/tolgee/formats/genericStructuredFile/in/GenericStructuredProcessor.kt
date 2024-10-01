package io.tolgee.formats.genericStructuredFile.`in`

import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.MessageConvertorResult
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.service.dataImport.processors.FileProcessorContext

class GenericStructuredProcessor(
  override val context: FileProcessorContext,
  private val data: Any?,
  private val convertor: StructuredRawDataConvertor,
  private val languageTag: String? = null,
  private val format: ImportFormat,
) : ImportFileProcessor() {
  override fun process() {
    var processedData = data
    if (format.pluralsViaSuffixesParser != null) {
      processedData =
        GenericSuffixedPluralsPreprocessor(
          context = context,
          data = data,
          pluralsViaSuffixesParser = format.pluralsViaSuffixesParser,
        ).preprocess()
    }
    processedData.import("")
  }

  private fun Any?.import(key: String) {
    // Convertor handles strings and possible nested plurals, if convertor returns null,
    // it means that it's not a string or nested plurals, so we need to parse it further
    convert(this)?.let {
      it.applyAll(key, this@import)
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

  private fun MessageConvertorResult.apply(
    key: String,
    rawData: Any?,
  ) {
    context.addTranslation(
      keyName = key,
      languageName = languageTagOrGuess,
      value = message,
      rawData = rawData,
      convertedBy = format,
      pluralArgName = pluralArgName,
    )
  }

  private fun List<MessageConvertorResult>.applyAll(
    key: String,
    rawData: Any?,
  ) {
    forEach {
      it.apply(key, rawData)
    }
  }

  private val languageTagOrGuess: String by lazy {
    languageTag ?: firstLanguageTagGuessOrUnknown
  }
}
