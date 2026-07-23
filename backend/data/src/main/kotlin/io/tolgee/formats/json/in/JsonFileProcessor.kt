package io.tolgee.formats.json.`in`

import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.genericStructuredFile.`in`.GenericStructuredProcessor
import io.tolgee.formats.genericStructuredFile.`in`.GenericStructuredRawDataToTextConvertor
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.service.dataImport.processors.FileProcessorContext
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

class JsonFileProcessor(
  override val context: FileProcessorContext,
  private val objectMapper: ObjectMapper,
) : ImportFileProcessor() {
  override fun process() {
    val data =
      try {
        objectMapper.readValue<Any?>(context.file.data)
      } catch (e: Exception) {
        throw ImportCannotParseFileException(context.file.name, e.message, e)
      }
    val format = getFormat(data)
    GenericStructuredProcessor(
      context = context,
      data = data,
      format = format,
      convertor =
        GenericStructuredRawDataToTextConvertor(
          format = format,
          firstLanguageTagGuessOrUnknown,
        ),
    ).process()
  }

  private fun getFormat(data: Any?): ImportFormat {
    return context.mapping?.format ?: JsonImportFormatDetector().detectFormat(data)
  }
}
