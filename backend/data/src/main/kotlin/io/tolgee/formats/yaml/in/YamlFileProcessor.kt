package io.tolgee.formats.yaml.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.genericStructuredFile.`in`.GenericStructuredProcessor
import io.tolgee.formats.genericStructuredFile.`in`.GenericStructuredRawDataToTextConvertor
import io.tolgee.formats.importMessageFormat.ImportMessageFormat
import io.tolgee.service.dataImport.processors.FileProcessorContext

class YamlFileProcessor(
  override val context: FileProcessorContext,
  private val objectMapper: ObjectMapper,
) : ImportFileProcessor() {
  override fun process() {
    val data = objectMapper.readValue<Any?>(context.file.data)
    val dataMap = data as? Map<*, *> ?: return
    val detectedFormat = YamlImportFormatDetector().detectFormat(dataMap)
    context.fileEntity.format = detectedFormat
    if (detectedFormat.rootKeyIsLanguageTag) {
      dataMap.entries.forEach { (languageTag, languageData) ->
        if (languageTag !is String) return@forEach
        processLanguageData(detectedFormat, languageTag, languageData)
      }
      return
    }
    processLanguageData(detectedFormat, firstLanguageTagGuessOrUnknown, data)
  }

  private fun processLanguageData(
    detectedFormat: ImportMessageFormat,
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
    ).process()
  }
}
