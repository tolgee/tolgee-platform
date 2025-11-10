package io.tolgee.formats.properties.`in`

import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.MessageConvertorResult
import io.tolgee.service.dataImport.processors.FileProcessorContext
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.io.FileHandler

class PropertiesFileProcessor(
  override val context: FileProcessorContext,
) : ImportFileProcessor() {
  override fun process() {
    try {
      keyValueMap.onEachIndexed { idx, (key, value) ->
        val comment = preparedConfig.layout.getCanonicalComment(key, false)
        if (!comment.isNullOrBlank()) {
          context.addKeyDescription(key, comment)
        }
        convert(value).let {
          context.addTranslation(
            key,
            firstLanguageTagGuessOrUnknown,
            it.message,
            rawData = value,
            convertedBy = format,
            pluralArgName = it.pluralArgName,
          )
        }
      }
    } catch (e: Exception) {
      throw ImportCannotParseFileException(context.file.name, e.message, e)
    }
  }

  private val preparedConfig by lazy {
    val config = PropertiesConfiguration()
    val handler = FileHandler(config)
    handler.load(context.file.data.inputStream())
    config
  }

  private val keyValueMap by lazy {
    preparedConfig.keys
      .asSequence()
      .map { key -> key to preparedConfig.getString(key) }
      .toMap(LinkedHashMap())
  }

  private val format by lazy {
    context.mapping?.format
      ?: PropertiesImportFormatDetector().detectFormat(keyValueMap)
  }

  private val convertor by lazy {
    format.messageConvertor
  }

  private fun convert(data: Any?): MessageConvertorResult {
    return convertor.convert(
      rawData = data,
      languageTag = firstLanguageTagGuessOrUnknown,
      isProjectIcuEnabled = context.projectIcuPlaceholdersEnabled,
      convertPlaceholders = context.importSettings.convertPlaceholdersToIcu,
    )
  }
}
