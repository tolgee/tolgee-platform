package io.tolgee.formats.yaml.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.genericStructuredFile.`in`.GenericStructuredProcessor
import io.tolgee.formats.genericStructuredFile.`in`.GenericStructuredRawDataToTextConvertor
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.service.dataImport.processors.FileProcessorContext

class YamlFileProcessor(
  override val context: FileProcessorContext,
  private val objectMapper: ObjectMapper,
) : ImportFileProcessor() {
  override fun process() {
    val data =
      try {
        objectMapper.readValue<Any?>(context.file.data)
      } catch (e: Exception) {
        throw ImportCannotParseFileException(context.file.name, e.message ?: "", e)
      }
    val dataMap = data as? Map<*, *> ?: return
    val detectedFormat = getFormat(dataMap)
    if (detectedFormat.rootKeyIsLanguageTag) {
      dataMap.entries.forEach { (languageTag, languageData) ->
        if (languageTag !is String) return@forEach
        processLanguageData(detectedFormat, languageTag, languageData)
      }
      return
    }
    processLanguageData(detectedFormat, firstLanguageTagGuessOrUnknown, data)
  }

  private fun getFormat(dataMap: Map<*, *>): ImportFormat {
    return context.mapping?.format ?: YamlImportFormatDetector().detectFormat(dataMap)
  }

  private fun processLanguageData(
    detectedFormat: ImportFormat,
    languageTag: String,
    languageData: Any?,
  ) {
    val convertor =
      GenericStructuredRawDataToTextConvertor(
        detectedFormat,
        languageTag,
      )
    GenericStructuredProcessor(
      context = context,
      data = languageData,
      convertor = convertor,
      languageTag = languageTag,
      format = detectedFormat,
    ).process()
  }
}
